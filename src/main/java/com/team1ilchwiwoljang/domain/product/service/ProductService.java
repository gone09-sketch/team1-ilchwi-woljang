package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return productRepository.findByCategoryId(categoryId).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> getProducts(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepository.findByStatus(ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::from);
    }

    public Page<ProductResponse> getProductsByCategory(Long categoryId, String sort, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::from);
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "price_high" -> Sort.by("price").descending();
            case "price_low" -> Sort.by("price").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
