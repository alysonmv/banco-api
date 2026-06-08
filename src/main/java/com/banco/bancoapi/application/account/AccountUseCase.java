package com.banco.bancoapi.application.account;

import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.exception.ValidationException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.domain.model.Money;
import com.banco.bancoapi.domain.port.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Contas: criar e consultar. */
@Service
public class AccountUseCase {

    private final AccountRepository accountRepository;

    public AccountUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account create(String name, BigDecimal initialBalance) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("nome da conta e obrigatorio");
        }
        Money balance = Money.of(initialBalance == null ? BigDecimal.ZERO : initialBalance);
        if (balance.isNegative()) {
            throw new ValidationException("saldo inicial nao pode ser negativo");
        }
        return accountRepository.save(new Account(null, name, balance));
    }

    @Transactional(readOnly = true)
    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
