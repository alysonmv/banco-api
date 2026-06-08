package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.PagedResult;

/** Port do ledger imutavel de movimentacoes. */
public interface MovementRepository {

    Movement save(Movement movement);

    /** Historico paginado de uma conta (como origem ou destino), mais recentes primeiro. */
    PagedResult<Movement> findByAccountId(Long accountId, int page, int size);
}
