package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductDetailResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("카테고리 ID로 상품 목록을 정상적으로 조회한다.")
    void getProductsByCategory() {
        // given
        Long categoryId = 1L;
        Category category = Category.create("상의", null);
        Product product1 = Product.create("티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", category);
        Product product2 = Product.create("맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨", category);

        given(categoryRepository.existsById(categoryId)).willReturn(true);
        given(productRepository.findByCategoryId(categoryId)).willReturn(List.of(product1, product2));

        // when
        List<ProductResponse> result = productService.getProductsByCategory(categoryId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("티셔츠");
        assertThat(result.get(1).name()).isEqualTo("맨투맨");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회 시 예외가 발생한다.")
    void getProductsByNotExistsCategory() {
        // given
        Long invalidCategoryId = 999L;
        given(categoryRepository.existsById(invalidCategoryId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.getProductsByCategory(invalidCategoryId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 ID로 상품 상세 정보를 정상적으로 조회한다")
    void given_existingProductId_whenGetProductDetail_thenReturnProductDetail() {
        // given
        Long productId = 1L;
        Category category = Category.create("상의", null);
        ReflectionTestUtils.setField(category, "id", 10L);
        Product product = Product.create("티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", category);
        ReflectionTestUtils.setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductDetailResponse result = productService.getProductDetail(productId);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("티셔츠");
        assertThat(result.description()).isEqualTo("편안한 티셔츠");
        assertThat(result.price()).isEqualTo(10000);
        assertThat(result.stock()).isEqualTo(100);
        assertThat(result.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(result.categoryId()).isEqualTo(10L);
        assertThat(result.categoryName()).isEqualTo("상의");
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 조회 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void given_nonExistentProductId_whenGetProductDetail_thenThrowProductNotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
