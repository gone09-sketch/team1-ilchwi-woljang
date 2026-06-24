package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("전체 ON_SALE 상품 목록을 페이지네이션으로 조회한다.")
    void getProducts() {
        // given
        Category category = Category.create("상의", null);
        Product product1 = Product.create("티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", category);
        Product product2 = Product.create("맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨", category);

        given(productRepository.findByStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product1, product2)));

        // when
        Page<ProductResponse> result = productService.getProducts("newest", 0, 20);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("티셔츠");
        assertThat(result.getContent().get(1).name()).isEqualTo("맨투맨");
    }

    @Test
    @DisplayName("카테고리 ID로 ON_SALE 상품 목록을 페이지네이션으로 조회한다.")
    void getProductsByCategory() {
        // given
        Long categoryId = 1L;
        Category category = Category.create("상의", null);
        Product product1 = Product.create("티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", category);
        Product product2 = Product.create("맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨", category);

        given(categoryRepository.existsById(categoryId)).willReturn(true);
        given(productRepository.findByCategoryIdAndStatus(eq(categoryId), eq(ProductStatus.ON_SALE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product1, product2)));

        // when
        Page<ProductResponse> result = productService.getProductsByCategory(categoryId, "newest", 0, 20);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("티셔츠");
        assertThat(result.getContent().get(1).name()).isEqualTo("맨투맨");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회 시 예외가 발생한다.")
    void getProductsByNotExistsCategory() {
        // given
        Long invalidCategoryId = 999L;
        given(categoryRepository.existsById(invalidCategoryId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.getProductsByCategory(invalidCategoryId, "newest", 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
