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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sob concorrencia: varias transferencias ao mesmo tempo entre as mesmas contas.
 * O que importa: a soma dos saldos no fim e igual a do inicio (nao some nem aparece dinheiro)
 * e nenhum saldo fica negativo.
 */
class ConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    AccountUseCase accountUseCase;
    @Autowired
    ExecuteTransferUseCase transferUseCase;

    @Test
    void transferenciasConcorrentesPreservamASomaENaoFicamNegativas() throws Exception {
        int transfers = 300;
        BigDecimal each = new BigDecimal("1.00");

        Account a = accountUseCase.create("A", new BigDecimal("1000.00"));
        Account b = accountUseCase.create("B", new BigDecimal("1000.00"));
        BigDecimal initialSum = new BigDecimal("2000.00");

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < transfers; i++) {
            tasks.add(() -> {
                start.await();
                transferUseCase.execute(new TransferCommand(a.id(), b.id(), each, null));
                success.incrementAndGet();
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

        BigDecimal balanceA = accountUseCase.getById(a.id()).balance().value();
        BigDecimal balanceB = accountUseCase.getById(b.id()).balance().value();

        assertThat(success.get()).isEqualTo(transfers);
        assertThat(balanceA).isEqualByComparingTo("700.00");   // 1000 - 300
        assertThat(balanceB).isEqualByComparingTo("1300.00");  // 1000 + 300
        assertThat(balanceA.add(balanceB)).isEqualByComparingTo(initialSum);
        assertThat(balanceA.signum()).isGreaterThanOrEqualTo(0);
        assertThat(balanceB.signum()).isGreaterThanOrEqualTo(0);
    }
}
