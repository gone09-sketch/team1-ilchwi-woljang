package com.team1ilchwiwoljang.domain.product.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductSearchResponse(
        List<ProductSearchItemResponse> products,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ProductSearchResponse from(Page<Product> page) {
        List<ProductSearchItemResponse> products = page.getContent().stream()
                .map(ProductSearchItemResponse::from)
                .toList();

        return new ProductSearchResponse(
                products,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
