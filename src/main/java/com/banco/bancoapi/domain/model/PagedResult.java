package com.banco.bancoapi.domain.model;

import java.util.List;

/** Paginacao em Java puro, pra nao amarrar o dominio no Spring Data. */
public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
