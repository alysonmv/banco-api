package com.banco.bancoapi.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Pedido de transferencia entre contas")
public record TransferRequest(

        @Schema(description = "Conta de origem (sera debitada)", example = "1")
        @NotNull(message = "fromAccountId e obrigatorio")
        Long fromAccountId,

        @Schema(description = "Conta de destino (sera creditada)", example = "2")
        @NotNull(message = "toAccountId e obrigatorio")
        Long toAccountId,

        @Schema(description = "Valor a transferir (> 0, no maximo 2 casas decimais)", example = "150.00")
        @NotNull(message = "amount e obrigatorio")
        @Positive(message = "amount deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "amount deve ter no maximo 2 casas decimais")
        BigDecimal amount
) {
}
