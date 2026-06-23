package com.team1ilchwiwoljang.domain.category.dto.response;

import com.team1ilchwiwoljang.domain.category.entity.Category;

import java.util.List;

public record CategoryResponse(
        Long categoryId,
        String name,
        List<CategoryResponse> children
) {

    public static CategoryResponse fromRoot(Category root) {
        return new CategoryResponse(
                root.getId(),
                root.getName(),
                root.getChildren().stream()
                        .map(CategoryResponse::fromChild)
                        .toList()
        );
    }

    private static CategoryResponse fromChild(Category child) {
        return new CategoryResponse(
                child.getId(),
                child.getName(),
                List.of()
        );
    }
}
