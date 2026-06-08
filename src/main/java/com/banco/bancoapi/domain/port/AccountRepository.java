package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.Account;

import java.util.Optional;

/** Port de persistencia de contas. A implementacao vive na infraestrutura. */
public interface AccountRepository {

    Optional<Account> findById(Long id);

    /**
     * Carrega a conta sob lock pessimista de escrita (SELECT ... FOR UPDATE).
     * Deve ser chamado dentro de uma transacao. E a base da correcao sob concorrencia:
     * nenhuma outra transacao le/escreve a mesma conta enquanto este lock estiver ativo.
     */
    Optional<Account> findByIdForUpdate(Long id);

    /** Cria uma nova conta ou persiste alteracoes de saldo de uma existente. */
    Account save(Account account);
}
