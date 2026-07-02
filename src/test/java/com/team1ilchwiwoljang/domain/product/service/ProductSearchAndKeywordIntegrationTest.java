package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import com.team1ilchwiwoljang.domain.search.service.SearchKeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import com.team1ilchwiwoljang.common.response.PageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductSearchAndKeywordIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @MockitoBean
    private SearchKeywordService mockSearchKeywordService;

    @Test
    @DisplayName("상품 검색 시 검색어 집계 기능이 에러를 던지더라도 상품 검색 자체는 차단 없이 정상 반환된다.")
    void givenKeywordIncrementServiceThrowsException_whenSearchProducts_thenSearchStillSucceeds() {
        // given: 상품 데이터 준비
        Category category = categoryRepository.save(Category.create("상의", null));
        Product product = Product.create("베이직 셔츠", 10000, 100, ProductStatus.ON_SALE, "설명", category);
        productRepository.save(product);

        // searchKeywordService 가 어떤 에러든 던지도록 설정
        doThrow(new RuntimeException("Database error in statistics tracking"))
                .when(mockSearchKeywordService).incrementSearchCount(anyString());

        // when
        PageResponse<ProductSearchItemResponse> response = productService.searchProducts("셔츠", PageRequest.of(0, 10));

        // then: 에러가 무시되고 상품 검색 결과가 정상 반환되어야 함
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("베이직 셔츠");
    }
}
