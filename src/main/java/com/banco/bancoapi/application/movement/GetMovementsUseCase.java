package com.banco.bancoapi.application.movement;

import com.banco.bancoapi.domain.exception.AccountNotFoundException;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.PagedResult;
import com.banco.bancoapi.domain.port.AccountRepository;
import com.banco.bancoapi.domain.port.MovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta paginada do historico de movimentacoes de uma conta (le do ledger). */
@Service
public class GetMovementsUseCase {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;

    public GetMovementsUseCase(MovementRepository movementRepository, AccountRepository accountRepository) {
        this.movementRepository = movementRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<Movement> execute(Long accountId, int page, int size) {
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        return movementRepository.findByAccountId(accountId, page, size);
    }
}
