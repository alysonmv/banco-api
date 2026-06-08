package com.banco.bancoapi.infrastructure.web.dto;

import com.banco.bancoapi.domain.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Representacao de uma conta")
public record AccountResponse(

        @Schema(description = "Id da conta", example = "1")
        Long id,

        @Schema(description = "Nome do titular", example = "Alice")
        String name,

        @Schema(description = "Saldo atual", example = "1000.00")
        BigDecimal balance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id(), account.name(), account.balance().value());
    }
}
