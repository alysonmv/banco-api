package com.banco.bancoapi.infrastructure.web;

import com.banco.bancoapi.application.transfer.ExecuteTransferUseCase;
import com.banco.bancoapi.application.transfer.TransferCommand;
import com.banco.bancoapi.application.transfer.TransferResult;
import com.banco.bancoapi.infrastructure.web.dto.TransferRequest;
import com.banco.bancoapi.infrastructure.web.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@Tag(name = "Transferencias", description = "Transferencia atomica de valores entre contas")
public class TransferController {

    private final ExecuteTransferUseCase executeTransferUseCase;

    public TransferController(ExecuteTransferUseCase executeTransferUseCase) {
        this.executeTransferUseCase = executeTransferUseCase;
    }

    @PostMapping
    @Operation(summary = "Executa uma transferencia",
            description = "Atomica e segura sob concorrencia (lock pessimista). Use o header "
                    + "Idempotency-Key para tornar a operacao idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transferencia concluida"),
            @ApiResponse(responseCode = "400", description = "Valor invalido ou mesma conta"),
            @ApiResponse(responseCode = "404", description = "Conta inexistente"),
            @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "Chave de idempotencia (opcional)", example = "a1b2c3d4")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        TransferCommand command = new TransferCommand(
                request.fromAccountId(), request.toAccountId(), request.amount(), idempotencyKey);
        TransferResult result = executeTransferUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(result));
    }
}
