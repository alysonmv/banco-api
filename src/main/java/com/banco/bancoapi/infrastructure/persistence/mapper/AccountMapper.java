package com.banco.bancoapi.infrastructure.persistence.mapper;

import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.domain.model.Money;
import com.banco.bancoapi.infrastructure.persistence.entity.AccountEntity;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static Account toDomain(AccountEntity entity) {
        return new Account(entity.getId(), entity.getName(), Money.of(entity.getBalance()));
    }
}
