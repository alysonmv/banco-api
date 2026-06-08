package com.banco.bancoapi.infrastructure.web.dto;

import com.banco.bancoapi.application.transfer.TransferResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Registro de uma transferencia concluida")
public record TransferResponse(

        @Schema(description = "Id da movimentacao no ledger", example = "10")
        Long movementId,

        @Schema(example = "1")
        Long fromAccountId,

        @Schema(example = "2")
        Long toAccountId,

        @Schema(example = "150.00")
        BigDecimal amount,

        @Schema(description = "Status da movimentacao", example = "COMPLETED")
        String status,

        @Schema(description = "Momento da conclusao (UTC)")
        Instant createdAt
) {
    public static TransferResponse from(TransferResult r) {
        return new TransferResponse(
                r.movementId(), r.fromAccountId(), r.toAccountId(), r.amount(), r.status(), r.createdAt());
    }
}
