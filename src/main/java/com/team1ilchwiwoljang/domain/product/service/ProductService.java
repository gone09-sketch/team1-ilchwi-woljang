package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductDetailResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 상품 엔티티 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 주문 재고 차감
     */
    @Transactional(readOnly = true)
    public Product getProductWithPessimisticLock(Long productId){
        return productRepository.findByWithPessimisticLock(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 상품 상세 조회
     */
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return ProductDetailResponse.from(product);
    }

    /**
     * 판매 중인 상품 목록 조회
     * newest(기본), price_high, price_low 정렬을 지원합니다.
     */
    public Page<ProductResponse> getProducts(String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));

        return productRepository.findByStatus(ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::from);
    }

    /**
     * 카테고리별 판매 중인 상품 목록 조회
     */
    public Page<ProductResponse> getProductsByCategory(
            Long categoryId,
            String sort,
            int page,
            int size
    ) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));

        return productRepository.findByCategoryIdAndStatus(
                        categoryId,
                        ProductStatus.ON_SALE,
                        pageable
                )
                .map(ProductResponse::from);
    }

    /**
     * 상품명 기반 검색
     * 판매 중지 상품은 검색 결과에서 제외합니다.
     */
    public PageResponse<ProductSearchItemResponse> searchProducts(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim();

        if (normalizedKeyword.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        return PageResponse.from(
                productRepository.findByNameContainingIgnoreCaseAndStatusNot(
                        normalizedKeyword,
                        ProductStatus.STOPPED,
                        pageable
                ).map(ProductSearchItemResponse::from)
        );
    }

    /**
     * 상품 목록 정렬 조건 변환
     */
    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "price_high" ->
                    Sort.by("price").descending();

            case "price_low" ->
                    Sort.by("price").ascending();

            default ->
                    Sort.by("createdAt").descending();
        };
    }

}