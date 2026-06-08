package com.banco.bancoapi.infrastructure.persistence.adapter;

import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.model.Account;
import com.banco.bancoapi.domain.port.AccountRepository;
import com.banco.bancoapi.infrastructure.persistence.entity.AccountEntity;
import com.banco.bancoapi.infrastructure.persistence.jpa.AccountJpaRepository;
import com.banco.bancoapi.infrastructure.persistence.mapper.AccountMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;

    public AccountRepositoryAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpa.findById(id).map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(AccountMapper::toDomain);
    }

    @Override
    public Account save(Account account) {
        if (account.id() == null) {
            AccountEntity entity = new AccountEntity(null, account.name(), account.balance().value());
            return AccountMapper.toDomain(jpa.save(entity));
        }
        // conta ja existe: a entidade ta gerenciada no contexto (foi carregada com lock).
        // mexo so no saldo; createdAt e o resto ficam como estao.
        AccountEntity entity = jpa.findById(account.id())
                .orElseThrow(() -> new AccountNotFoundException(account.id()));
        entity.setBalance(account.balance().value());
        return AccountMapper.toDomain(entity);
    }
}
