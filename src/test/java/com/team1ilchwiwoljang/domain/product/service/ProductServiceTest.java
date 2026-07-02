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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
        ReflectionTestUtils.setField(category, "id", categoryId);
        Product product1 = Product.create("티셔츠", 10000, 100, ProductStatus.ON_SALE, "편안한 티셔츠", category);
        Product product2 = Product.create("맨투맨", 20000, 50, ProductStatus.ON_SALE, "따뜻한 맨투맨", category);

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(productRepository.findByCategoryIdInAndStatus(eq(List.of(categoryId)), eq(ProductStatus.ON_SALE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product1, product2)));

        // when
        Page<ProductResponse> result = productService.getProductsByCategory(categoryId, "newest", 0, 20);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("티셔츠");
        assertThat(result.getContent().get(1).name()).isEqualTo("맨투맨");
    }

    @Test
    @DisplayName("루트 카테고리 ID로 조회하면 자식 카테고리 상품까지 함께 조회한다.")
    void getProductsByRootCategoryWithChildren() {
        // given
        Long rootCategoryId = 1L;
        Long childCategoryId = 2L;
        Category rootCategory = Category.createRoot("패션/잡화");
        ReflectionTestUtils.setField(rootCategory, "id", rootCategoryId);
        Category childCategory = Category.createChild("신발", rootCategory);
        ReflectionTestUtils.setField(childCategory, "id", childCategoryId);
        Product product = Product.create("운동화", 59000, 20, ProductStatus.ON_SALE, "편한 운동화", childCategory);

        given(categoryRepository.findById(rootCategoryId)).willReturn(Optional.of(rootCategory));
        given(productRepository.findByCategoryIdInAndStatus(
                eq(List.of(rootCategoryId, childCategoryId)),
                eq(ProductStatus.ON_SALE),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(product)));

        // when
        Page<ProductResponse> result = productService.getProductsByCategory(rootCategoryId, "newest", 0, 20);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("운동화");
        then(productRepository).should().findByCategoryIdInAndStatus(
                eq(List.of(rootCategoryId, childCategoryId)),
                eq(ProductStatus.ON_SALE),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 조회 시 예외가 발생한다.")
    void getProductsByNotExistsCategory() {
        // given
        Long invalidCategoryId = 999L;
        given(categoryRepository.findById(invalidCategoryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductsByCategory(invalidCategoryId, "newest", 0, 20))
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

    @Test
    @DisplayName("상품 ID로 상품 엔티티를 정상 조회한다")
    void given_existingProductId_whenGetProduct_thenReturnProduct() {
        // given
        Long productId = 1L;
        Product product = Product.create("keyboard", 10000, 10, ProductStatus.ON_SALE, "description", null);
        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        Product result = productService.getProduct(productId);

        // then
        assertThat(result).isEqualTo(product);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 상품 엔티티를 조회하면 PRODUCT_NOT_FOUND 예외가 발생한다")
    void given_nonExistentProductId_whenGetProduct_thenThrowProductNotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
