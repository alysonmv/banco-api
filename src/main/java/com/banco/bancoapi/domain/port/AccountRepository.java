package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.Account;

import java.util.Optional;

/** Persistencia de contas. A implementacao fica na infra. */
public interface AccountRepository {

    Optional<Account> findById(Long id);

    /**
     * Carrega a conta com lock de escrita (SELECT ... FOR UPDATE). Tem que rodar dentro de
     * uma transacao. E o que segura a concorrencia: ninguem mexe na conta enquanto o lock
     * estiver de pe.
     */
    Optional<Account> findByIdForUpdate(Long id);

    /** Cria uma conta nova ou salva o saldo de uma que ja existe. */
    Account save(Account account);
}
