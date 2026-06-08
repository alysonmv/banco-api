package com.banco.bancoapi.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Corpo padrao de erro")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        @Schema(description = "Momento do erro (UTC)")
        Instant timestamp,

        @Schema(description = "HTTP status", example = "422")
        int status,

        @Schema(description = "Codigo estavel do erro", example = "INSUFFICIENT_BALANCE")
        String code,

        @Schema(description = "Mensagem legivel", example = "Saldo insuficiente na conta 1")
        String message,

        @Schema(description = "Correlation id da requisicao")
        String correlationId,

        @Schema(description = "Detalhes de validacao por campo (quando aplicavel)")
        List<FieldError> errors
) {
    @Schema(description = "Erro de validacao de um campo")
    public record FieldError(String field, String message) {
    }
}
