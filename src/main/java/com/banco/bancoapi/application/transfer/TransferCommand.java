package com.banco.bancoapi.application.transfer;

import java.math.BigDecimal;

/** Entrada da transferencia. idempotencyKey e opcional (vem do header Idempotency-Key). */
public record TransferCommand(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String idempotencyKey
) {
}
