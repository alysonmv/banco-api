package com.banco.bancoapi.infrastructure.web;

import com.banco.bancoapi.application.movement.GetMovementsUseCase;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.PagedResult;
import com.banco.bancoapi.infrastructure.web.dto.MovementResponse;
import com.banco.bancoapi.infrastructure.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/{id}/movements")
@Tag(name = "Movimentacoes", description = "Historico de movimentacoes de uma conta")
public class MovementController {

    private final GetMovementsUseCase getMovementsUseCase;

    public MovementController(GetMovementsUseCase getMovementsUseCase) {
        this.getMovementsUseCase = getMovementsUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista o historico paginado de movimentacoes da conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de movimentacoes"),
            @ApiResponse(responseCode = "404", description = "Conta inexistente")
    })
    public ResponseEntity<PageResponse<MovementResponse>> list(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResult<Movement> result = getMovementsUseCase.execute(id, page, size);
        return ResponseEntity.ok(PageResponse.from(result, MovementResponse::from));
    }
}
