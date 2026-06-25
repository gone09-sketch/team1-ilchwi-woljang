package com.team1ilchwiwoljang.domain.product.dto.response;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductSearchResponse(
        List<ProductSearchItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static ProductSearchResponse from(Page<Product> page) {
        List<ProductSearchItemResponse> content = page.getContent().stream()
                .map(ProductSearchItemResponse::from)
                .toList();

        return new ProductSearchResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
