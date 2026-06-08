package com.banco.bancoapi;

import com.banco.bancoapi.application.account.AccountUseCase;
import com.banco.bancoapi.application.movement.GetMovementsUseCase;
import com.banco.bancoapi.application.transfer.ExecuteTransferUseCase;
import com.banco.bancoapi.application.transfer.TransferCommand;
import com.banco.bancoapi.application.transfer.TransferResult;
import com.banco.bancoapi.domain.exception.IdempotencyConflictException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Idempotencia: a mesma Idempotency-Key nao roda de novo nem duplica a transferencia. */
class IdempotencyIT extends AbstractIntegrationTest {

    @Autowired
    AccountUseCase accountUseCase;
    @Autowired
    ExecuteTransferUseCase transferUseCase;
    @Autowired
    GetMovementsUseCase movementsUseCase;

    @Test
    void mesmaChaveDuasVezesNaoDuplica() {
        Account from = accountUseCase.create("From", new BigDecimal("100.00"));
        Account to = accountUseCase.create("To", new BigDecimal("0.00"));
        TransferCommand cmd = new TransferCommand(from.id(), to.id(), new BigDecimal("40.00"), "key-1");

        TransferResult first = transferUseCase.execute(cmd);
        TransferResult second = transferUseCase.execute(cmd);

        assertThat(second.movementId()).isEqualTo(first.movementId());
        assertThat(accountUseCase.getById(from.id()).balance().value()).isEqualByComparingTo("60.00");
        assertThat(accountUseCase.getById(to.id()).balance().value()).isEqualByComparingTo("40.00");
        assertThat(movementsUseCase.execute(from.id(), 0, 50).content()).hasSize(1);
    }

    @Test
    void mesmaChaveComPayloadDivergenteRetornaConflito() {
        Account from = accountUseCase.create("From2", new BigDecimal("100.00"));
        Account to = accountUseCase.create("To2", new BigDecimal("0.00"));
        transferUseCase.execute(new TransferCommand(from.id(), to.id(), new BigDecimal("10.00"), "key-2"));

        assertThatThrownBy(() -> transferUseCase.execute(
                new TransferCommand(from.id(), to.id(), new BigDecimal("99.00"), "key-2")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void mesmaChaveSobConcorrenciaAplicaUmaUnicaVez() throws Exception {
        Account from = accountUseCase.create("From3", new BigDecimal("100.00"));
        Account to = accountUseCase.create("To3", new BigDecimal("0.00"));
        TransferCommand cmd = new TransferCommand(from.id(), to.id(), new BigDecimal("25.00"), "key-3");

        int n = 12;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> movementIds = ConcurrentHashMap.newKeySet();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tasks.add(() -> {
                start.await();
                movementIds.add(transferUseCase.execute(cmd).movementId());
                return null;
            });
        }
        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        start.countDown();
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(movementIds).hasSize(1); // todos devolvem a mesma movimentacao
        assertThat(accountUseCase.getById(from.id()).balance().value()).isEqualByComparingTo("75.00");
        assertThat(movementsUseCase.execute(from.id(), 0, 50).content()).hasSize(1);
    }
}
