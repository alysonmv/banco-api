package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.exception.IdempotencyConflictException;
import com.banco.bancoapi.domain.exception.InvalidTransferException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.domain.model.IdempotencyRecord;
import com.banco.bancoapi.domain.model.Money;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.port.AccountRepository;
import com.banco.bancoapi.domain.port.IdempotencyRepository;
import com.banco.bancoapi.domain.port.MovementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Caso de uso central: transferencia atomica entre contas.
 *
 * <p>Define a transacao ({@link Transactional}) e orquestra os ports. O coracao da correcao
 * sob concorrencia esta aqui: <b>os locks pessimistas das duas contas sao adquiridos sempre na
 * mesma ordem (id menor primeiro)</b>, o que elimina deadlock entre A&rarr;B e B&rarr;A. Saldo
 * suficiente e validado <b>dentro do lock</b>, antes de debitar, garantindo saldo nunca negativo.
 *
 * <p>Decisao pragmatica de Clean Architecture: a camada de aplicacao usa {@code @Transactional}
 * e {@link ObjectMapper}. O dominio permanece puro; a aplicacao pode depender de frameworks.
 */
@Service
public class ExecuteTransferUseCase {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransferEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ExecuteTransferUseCase(AccountRepository accountRepository,
                                  MovementRepository movementRepository,
                                  IdempotencyRepository idempotencyRepository,
                                  TransferEventPublisher eventPublisher,
                                  ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransferResult execute(TransferCommand command) {
        validate(command);
        Money amount = Money.of(command.amount());

        String key = normalizeKey(command.idempotencyKey());
        String requestHash = (key != null) ? hash(command) : null;

        // --- Idempotencia: replay rapido se a chave ja foi concluida ---
        if (key != null) {
            Optional<IdempotencyRecord> existing = idempotencyRepository.findByKey(key);
            if (existing.isPresent()) {
                return replay(existing.get(), requestHash);
            }
            // Reserva a chave (claim) ANTES de transferir. A PK serializa concorrentes.
            if (!idempotencyRepository.tryClaim(key, requestHash)) {
                // Outro request com a mesma chave venceu a corrida e ja commitou: devolve o dele.
                IdempotencyRecord winner = idempotencyRepository.findByKey(key)
                        .orElseThrow(() -> new IllegalStateException(
                                "claim falhou mas registro de idempotencia ausente: " + key));
                return replay(winner, requestHash);
            }
        }

        Movement movement = doTransfer(command, amount, key);
        TransferResult result = TransferResult.from(movement);

        if (key != null) {
            // 201 Created: mesmo status que o controller devolve, inclusive no replay.
            idempotencyRepository.complete(key, movement.id(), 201, serialize(result));
        }

        // Anuncia a conclusao. A publicacao real na fila ocorre APOS o commit (nao-critica).
        eventPublisher.transferCompleted(movement);

        return result;
    }

    private Movement doTransfer(TransferCommand command, Money amount, String key) {
        // Lock pessimista das duas contas SEMPRE na mesma ordem (id menor primeiro) -> sem deadlock.
        long firstId = Math.min(command.fromAccountId(), command.toAccountId());
        long secondId = Math.max(command.fromAccountId(), command.toAccountId());

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        Account from = command.fromAccountId().equals(first.id()) ? first : second;
        Account to = command.toAccountId().equals(first.id()) ? first : second;

        // Saldo validado dentro do lock (debit lanca InsufficientBalanceException se faltar).
        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        Movement movement = Movement.completed(
                command.fromAccountId(), command.toAccountId(), amount, key);
        return movementRepository.save(movement);
    }

    private void validate(TransferCommand command) {
        if (command.fromAccountId() == null || command.toAccountId() == null) {
            throw new InvalidTransferException("contas de origem e destino sao obrigatorias");
        }
        if (command.fromAccountId().equals(command.toAccountId())) {
            throw new InvalidTransferException("nao e permitido transferir para a mesma conta");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new InvalidTransferException("o valor da transferencia deve ser maior que zero");
        }
    }

    private TransferResult replay(IdempotencyRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(record.idempotencyKey());
        }
        return deserialize(record.responseBody());
    }

    private static String normalizeKey(String key) {
        if (key == null) return null;
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String hash(TransferCommand command) {
        String canonical = command.fromAccountId() + "|" + command.toAccountId() + "|"
                + Money.of(command.amount()).value().toPlainString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    private String serialize(TransferResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException("falha ao serializar resultado de idempotencia", e);
        }
    }

    private TransferResult deserialize(String body) {
        try {
            return objectMapper.readValue(body, TransferResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("falha ao desserializar resposta de idempotencia", e);
        }
    }
}
