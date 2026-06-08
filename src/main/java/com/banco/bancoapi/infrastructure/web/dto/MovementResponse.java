package com.banco.bancoapi.infrastructure.web.dto;

import com.banco.bancoapi.domain.model.Movement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Movimentacao do historico (ledger)")
public record MovementResponse(

        @Schema(description = "Id da movimentacao", example = "10")
        Long id,

        @Schema(description = "Conta de origem", example = "1")
        Long fromAccountId,

        @Schema(description = "Conta de destino", example = "2")
        Long toAccountId,

        @Schema(description = "Valor movimentado", example = "150.00")
        BigDecimal amount,

        @Schema(description = "Status da movimentacao", example = "COMPLETED")
        String status,

        @Schema(description = "Chave de idempotencia usada (se houver)", example = "a1b2c3d4")
        String idempotencyKey,

        @Schema(description = "Momento da movimentacao (UTC)")
        Instant createdAt
) {
    public static MovementResponse from(Movement m) {
        return new MovementResponse(
                m.id(), m.fromAccountId(), m.toAccountId(), m.amount().value(),
                m.status().name(), m.idempotencyKey(), m.createdAt());
    }
}
