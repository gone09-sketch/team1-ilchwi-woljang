package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void searchProductsReturnsMappedResponse() {
        Product product = Product.create("베이직 셔츠", 29_000, 10, ProductStatus.ON_SALE, "기본 셔츠", null);
        Pageable pageable = PageRequest.of(0, 20);

        given(productRepository.findByNameContainingIgnoreCaseAndStatusNot(
                "셔츠",
                ProductStatus.STOPPED,
                pageable
        )).willReturn(new PageImpl<>(List.of(product), pageable, 1));

        ProductSearchResponse response = productService.searchProducts(" 셔츠 ", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("베이직 셔츠");
        assertThat(response.content().get(0).price()).isEqualTo(29_000);
        assertThat(response.content().get(0).stock()).isEqualTo(10);
        assertThat(response.content().get(0).status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(response.content().get(0).orderable()).isTrue();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();
        then(productRepository).should().findByNameContainingIgnoreCaseAndStatusNot(
                "셔츠",
                ProductStatus.STOPPED,
                pageable
        );
    }

    @Test
    void searchProductsMapsOutOfStockAsNotOrderable() {
        Product product = Product.create("품절 셔츠", 19_000, 0, ProductStatus.OUT_OF_STOCK, "품절 상품", null);
        Pageable pageable = PageRequest.of(0, 20);

        given(productRepository.findByNameContainingIgnoreCaseAndStatusNot(
                "셔츠",
                ProductStatus.STOPPED,
                pageable
        )).willReturn(new PageImpl<>(List.of(product), pageable, 1));

        ProductSearchResponse response = productService.searchProducts("셔츠", pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThat(response.content().get(0).orderable()).isFalse();
    }

    @Test
    void searchProductsThrowsExceptionWhenKeywordIsBlank() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> productService.searchProducts("   ", pageable))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}
