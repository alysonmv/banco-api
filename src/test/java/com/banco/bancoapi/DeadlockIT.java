package com.banco.bancoapi;

import com.banco.bancoapi.application.account.AccountUseCase;
import com.banco.bancoapi.application.transfer.ExecuteTransferUseCase;
import com.banco.bancoapi.application.transfer.TransferCommand;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deadlock: A->B e B->A em paralelo, varias vezes. Como os locks saem sempre na mesma ordem
 * (id menor primeiro), nao tem espera circular. Tem que terminar dentro do timeout e os saldos
 * finais batem.
 */
class DeadlockIT extends AbstractIntegrationTest {

    @Autowired
    AccountUseCase accountUseCase;
    @Autowired
    ExecuteTransferUseCase transferUseCase;

    @Test
    void transferenciasCruzadasNaoCausamDeadlock() throws Exception {
        int pairs = 150; // 150 A->B e 150 B->A
        BigDecimal each = new BigDecimal("1.00");

        Account a = accountUseCase.create("A", new BigDecimal("10000.00"));
        Account b = accountUseCase.create("B", new BigDecimal("10000.00"));

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            tasks.add(() -> {
                start.await();
                transferUseCase.execute(new TransferCommand(a.id(), b.id(), each, null));
                return null;
            });
            tasks.add(() -> {
                start.await();
                transferUseCase.execute(new TransferCommand(b.id(), a.id(), each, null));
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        start.countDown();
        for (Future<Void> f : futures) {
            f.get(); // propaga qualquer falha (inclusive deadlock detectado pelo banco)
        }
        pool.shutdown();
        boolean finished = pool.awaitTermination(60, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        // mesma quantidade nos dois sentidos -> saldo volta ao inicial
        assertThat(accountUseCase.getById(a.id()).balance().value()).isEqualByComparingTo("10000.00");
        assertThat(accountUseCase.getById(b.id()).balance().value()).isEqualByComparingTo("10000.00");
    }
}
