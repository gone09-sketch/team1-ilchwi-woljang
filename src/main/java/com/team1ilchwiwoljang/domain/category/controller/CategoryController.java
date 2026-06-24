package com.team1ilchwiwoljang.domain.category.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.category.dto.response.CategoryResponse;
import com.team1ilchwiwoljang.domain.category.service.CategoryService;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    /**
     * 카테고리 전체 목록 조회 (계층 구조)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> response = categoryService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 카테고리에 속한 상품 목록 조회
     */
    @GetMapping("/{categoryId}/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductResponse> responses = PageResponse.from(productService.getProductsByCategory(categoryId, sort, page, size));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
