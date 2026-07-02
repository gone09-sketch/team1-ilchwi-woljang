package com.team1ilchwiwoljang.domain.product.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductDetailResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "newest") String sort,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductResponse> responses = PageResponse.from(productService.getProducts(sort, page, size));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @PathVariable Long productId
    ) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductSearchItemResponse>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductSearchItemResponse> response = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByPriceRange(
            @RequestParam int minPrice,
            @RequestParam int maxPrice,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductResponse> responses = PageResponse.from(productService.getProductsByPriceRange(minPrice, maxPrice, page, size));
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search-fulltext")
    public ResponseEntity<ApiResponse<PageResponse<ProductSearchItemResponse>>> searchProductsFullText(
            @RequestParam String keyword,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ProductSearchItemResponse> response = productService.searchProductsFullText(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularProductResponse>>> getPopularProducts(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        List<PopularProductResponse> response = productService.getPopularProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
