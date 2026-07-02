package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.response.ProductSearchItemResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import com.team1ilchwiwoljang.domain.search.entity.SearchKeyword;
import com.team1ilchwiwoljang.domain.search.repository.SearchKeywordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import com.team1ilchwiwoljang.common.response.PageResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductSearchAndKeywordRealIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("상품 검색 API를 정상적으로 호출하면 검색어가 수집되고 카운트가 증가한다.")
    void givenKeyword_whenSearchProductsReal_thenKeywordCountIncrements() {
        // given: 상품 데이터 준비
        Category category = categoryRepository.save(Category.create("상의", null));
        Product product = Product.create("베이직 셔츠", 10000, 100, ProductStatus.ON_SALE, "설명", category);
        productRepository.save(product);

        // when: 첫 번째 검색
        productService.searchProducts("셔츠", PageRequest.of(0, 10));
        entityManager.clear(); // 영속성 컨텍스트를 클리어하여 DB 최신값을 온전히 읽어오도록 캐시 차단

        // then: DB에 검색어가 등록되고 카운트가 1이 됨
        SearchKeyword keyword1 = searchKeywordRepository.findByKeyword("셔츠").orElseThrow();
        assertThat(keyword1.getSearchCount()).isEqualTo(1L);

        // when: 두 번째 검색
        productService.searchProducts("셔츠", PageRequest.of(0, 10));
        entityManager.clear(); // 영속성 컨텍스트를 클리어하여 DB 최신값을 온전히 읽어오도록 캐시 차단

        // then: 카운트가 2가 됨
        SearchKeyword keyword2 = searchKeywordRepository.findByKeyword("셔츠").orElseThrow();
        assertThat(keyword2.getSearchCount()).isEqualTo(2L);
    }
}
