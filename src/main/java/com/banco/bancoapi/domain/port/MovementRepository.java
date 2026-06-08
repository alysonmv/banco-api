package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.PagedResult;

/** Ledger de movimentacoes. */
public interface MovementRepository {

    Movement save(Movement movement);

    /** Historico paginado da conta (origem ou destino), mais novos primeiro. */
    PagedResult<Movement> findByAccountId(Long accountId, int page, int size);
}
