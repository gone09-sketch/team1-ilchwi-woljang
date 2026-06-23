package com.team1ilchwiwoljang.domain.category.service;

import com.team1ilchwiwoljang.domain.category.dto.response.CategoryResponse;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllRootWithChildren().stream()
                .map(CategoryResponse::fromRoot)
                .toList();
    }
}
