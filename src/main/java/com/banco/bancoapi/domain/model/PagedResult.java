package com.banco.bancoapi.domain.model;

import java.util.List;

/** Resultado paginado em Java puro — evita acoplar o dominio ao Spring Data. */
public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
