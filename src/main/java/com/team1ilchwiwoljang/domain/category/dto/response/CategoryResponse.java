package com.team1ilchwiwoljang.domain.category.dto.response;

import com.team1ilchwiwoljang.domain.category.entity.Category;

import java.util.List;

public record CategoryResponse(
        Long categoryId,
        String name,
        List<CategoryResponse> children
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .toList()
        );
    }
}
