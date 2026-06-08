package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.exception.IdempotencyConflictException;
import com.banco.bancoapi.domain.exception.InsufficientBalanceException;
import com.banco.bancoapi.domain.exception.InvalidTransferException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.domain.model.IdempotencyRecord;
import com.banco.bancoapi.domain.model.Money;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.MovementStatus;
import com.banco.bancoapi.domain.port.AccountRepository;
import com.banco.bancoapi.domain.port.IdempotencyRepository;
import com.banco.bancoapi.domain.port.MovementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecuteTransferUseCaseTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    MovementRepository movementRepository;
    @Mock
    IdempotencyRepository idempotencyRepository;
    @Mock
    TransferEventPublisher eventPublisher;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    ExecuteTransferUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecuteTransferUseCase(
                accountRepository, movementRepository, idempotencyRepository, eventPublisher, objectMapper);
    }

    @Test
    void transferenciaFelizDebitaCreditaRegistraEPublica() {
        Account from = new Account(1L, "Alice", Money.of("100.00"));
        Account to = new Account(2L, "Bob", Money.of("50.00"));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(to));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> withId((Movement) inv.getArgument(0), 99L));

        TransferResult result = useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("30.00"), null));

        assertThat(from.balance()).isEqualTo(Money.of("70.00"));
        assertThat(to.balance()).isEqualTo(Money.of("80.00"));
        assertThat(result.movementId()).isEqualTo(99L);
        assertThat(result.amount()).isEqualByComparingTo("30.00");
        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(eventPublisher, times(1)).transferCompleted(any());
    }

    @Test
    void locksAdquiridosSempreNaOrdemDoIdMenorPrimeiro() {
        // Transferencia de 2 -> 1: mesmo assim os locks devem ser pedidos na ordem 1, depois 2.
        Account a1 = new Account(1L, "Alice", Money.of("100.00"));
        Account a2 = new Account(2L, "Bob", Money.of("100.00"));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(a1));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(a2));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> withId((Movement) inv.getArgument(0), 1L));

        useCase.execute(new TransferCommand(2L, 1L, new BigDecimal("10.00"), null));

        var inOrder = org.mockito.Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdForUpdate(1L);
        inOrder.verify(accountRepository).findByIdForUpdate(2L);
    }

    @Test
    void mesmaContaRejeitada() {
        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 1L, new BigDecimal("10.00"), null)))
                .isInstanceOf(InvalidTransferException.class);
        verify(accountRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void valorNaoPositivoRejeitado() {
        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("0.00"), null)))
                .isInstanceOf(InvalidTransferException.class);
        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("-5.00"), null)))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void contaInexistenteLanca404() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new Account(1L, "A", Money.of("10.00"))));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("5.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void saldoInsuficienteLanca() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new Account(1L, "A", Money.of("3.00"))));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(new Account(2L, "B", Money.of("0.00"))));

        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("5.00"), null)))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void replayDeIdempotenciaNaoReexecuta() throws Exception {
        TransferResult original = new TransferResult(99L, 1L, 2L, new BigDecimal("30.00"), "COMPLETED", Instant.now());
        String body = objectMapper.writeValueAsString(original);
        String hash = hash(1L, 2L, "30.00");
        when(idempotencyRepository.findByKey("k1"))
                .thenReturn(Optional.of(new IdempotencyRecord("k1", hash, 99L, 200, body, Instant.now())));

        TransferResult result = useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("30.00"), "k1"));

        assertThat(result.movementId()).isEqualTo(99L);
        verify(accountRepository, never()).findByIdForUpdate(anyLong());
        verify(movementRepository, never()).save(any());
        verify(idempotencyRepository, never()).tryClaim(anyString(), anyString());
    }

    @Test
    void replayComPayloadDivergenteLanca409() {
        when(idempotencyRepository.findByKey("k1"))
                .thenReturn(Optional.of(new IdempotencyRecord("k1", "hash-diferente", 99L, 200, "{}", Instant.now())));

        assertThatThrownBy(() -> useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("30.00"), "k1")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void comChaveNovaReservaEPersisteResultado() {
        when(idempotencyRepository.findByKey("k2")).thenReturn(Optional.empty());
        when(idempotencyRepository.tryClaim(eq("k2"), anyString())).thenReturn(true);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new Account(1L, "A", Money.of("100.00"))));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(new Account(2L, "B", Money.of("0.00"))));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> withId((Movement) inv.getArgument(0), 7L));

        useCase.execute(new TransferCommand(1L, 2L, new BigDecimal("10.00"), "k2"));

        ArgumentCaptor<Long> movementId = ArgumentCaptor.forClass(Long.class);
        verify(idempotencyRepository).complete(eq("k2"), movementId.capture(), anyInt(), anyString());
        assertThat(movementId.getValue()).isEqualTo(7L);
    }

    private static Movement withId(Movement m, long id) {
        return new Movement(id, m.fromAccountId(), m.toAccountId(), m.amount(), m.status() == null
                ? MovementStatus.COMPLETED : m.status(), m.idempotencyKey(), Instant.now());
    }

    private static String hash(long from, long to, String amount) throws Exception {
        String canonical = from + "|" + to + "|" + Money.of(amount).value().toPlainString();
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
