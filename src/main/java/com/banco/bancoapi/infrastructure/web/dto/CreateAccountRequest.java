package com.banco.bancoapi.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastro de uma nova conta")
public record CreateAccountRequest(

        @Schema(description = "Nome do titular", example = "Alice")
        @NotBlank(message = "nome e obrigatorio")
        String name,

        @Schema(description = "Saldo inicial (>= 0, no maximo 2 casas decimais). Default 0 se ausente.", example = "1000.00")
        @PositiveOrZero(message = "saldo inicial nao pode ser negativo")
        @Digits(integer = 17, fraction = 2, message = "saldo inicial deve ter no maximo 2 casas decimais")
        BigDecimal initialBalance
) {
}
