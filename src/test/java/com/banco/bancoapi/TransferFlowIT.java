package com.banco.bancoapi;

import com.banco.bancoapi.application.account.AccountUseCase;
import com.banco.bancoapi.application.movement.GetMovementsUseCase;
import com.banco.bancoapi.application.transfer.ExecuteTransferUseCase;
import com.banco.bancoapi.application.transfer.TransferCommand;
import com.banco.bancoapi.application.transfer.TransferResult;
import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.exception.InsufficientBalanceException;
import com.banco.bancoapi.domain.exception.InvalidTransferException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferFlowIT extends AbstractIntegrationTest {

    @Autowired
    AccountUseCase accountUseCase;
    @Autowired
    ExecuteTransferUseCase transferUseCase;
    @Autowired
    GetMovementsUseCase movementsUseCase;

    @Test
    void transferenciaAtomicaAtualizaSaldosERegistraMovimentacao() {
        Account from = accountUseCase.create("Origem", new BigDecimal("200.00"));
        Account to = accountUseCase.create("Destino", new BigDecimal("50.00"));

        TransferResult result = transferUseCase.execute(
                new TransferCommand(from.id(), to.id(), new BigDecimal("75.50"), null));

        assertThat(result.movementId()).isNotNull();
        assertThat(accountUseCase.getById(from.id()).balance().value()).isEqualByComparingTo("124.50");
        assertThat(accountUseCase.getById(to.id()).balance().value()).isEqualByComparingTo("125.50");

        var page = movementsUseCase.execute(from.id(), 0, 10);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).amount().value()).isEqualByComparingTo("75.50");
    }

    @Test
    void saldoInsuficienteNaoAlteraSaldos() {
        Account from = accountUseCase.create("Pobre", new BigDecimal("10.00"));
        Account to = accountUseCase.create("Rico", new BigDecimal("0.00"));

        assertThatThrownBy(() -> transferUseCase.execute(
                new TransferCommand(from.id(), to.id(), new BigDecimal("10.01"), null)))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(accountUseCase.getById(from.id()).balance().value()).isEqualByComparingTo("10.00");
        assertThat(accountUseCase.getById(to.id()).balance().value()).isEqualByComparingTo("0.00");
    }

    @Test
    void contaInexistenteLancaNotFound() {
        Account from = accountUseCase.create("X", new BigDecimal("10.00"));
        assertThatThrownBy(() -> transferUseCase.execute(
                new TransferCommand(from.id(), 999999L, new BigDecimal("1.00"), null)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void mesmaContaRejeitada() {
        Account a = accountUseCase.create("Y", new BigDecimal("10.00"));
        assertThatThrownBy(() -> transferUseCase.execute(
                new TransferCommand(a.id(), a.id(), new BigDecimal("1.00"), null)))
                .isInstanceOf(InvalidTransferException.class);
    }
}
