package com.team1ilchwiwoljang.domain.product.service;

import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.dto.response.PopularProductResponse;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        // 캐시를 테스트 전에 매번 비워주어 테스트 격리성을 보장합니다.
        Cache cache = cacheManager.getCache("popularProducts");
        if (cache != null) {
            cache.clear();
        }

        Category category = categoryRepository.save(Category.create("상의", null));

        productA = Product.create("인기상품A", 10000, 100, ProductStatus.ON_SALE, "설명", category);
        productA.increaseSalesCount(150); // 누적 판매량 150
        productRepository.save(productA);

        productB = Product.create("인기상품B", 20000, 100, ProductStatus.ON_SALE, "설명", category);
        productB.increaseSalesCount(90); // 누적 판매량 90
        productRepository.save(productB);
    }

    @Test
    @DisplayName("인기 상품 조회 시 누적 판매량이 높은 순으로 상품 리스트가 반환된다.")
    void getPopularProductsOrderingTest() {
        // when
        List<PopularProductResponse> result = productService.getPopularProducts(10);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).productId()).isEqualTo(productA.getId()); // 누적 판매량 높은 A가 첫 번째
        assertThat(result.get(0).salesCount()).isEqualTo(150);
        assertThat(result.get(1).productId()).isEqualTo(productB.getId()); // B가 두 번째
        assertThat(result.get(1).salesCount()).isEqualTo(90);
    }

    @Test
    @DisplayName("인기 상품 조회 시 처음에는 DB를 조회하고 캐시에 저장되며, 두 번째부터는 캐시에서 직접 반환한다.")
    void popularProductsCachingBehaviorTest() {
        // 1. 캐시가 비어있는 상태인지 먼저 확인합니다.
        Cache cache = cacheManager.getCache("popularProducts");
        assertThat(cache).isNotNull();
        assertThat(cache.get(10)).isNull(); // key가 limit=10인 캐시 데이터 없음 확인

        // 2. 첫 번째 호출 (DB 조회 발생)
        List<PopularProductResponse> firstCall = productService.getPopularProducts(10);
        assertThat(firstCall).hasSize(2);

        // 3. 호출 후 캐시에 데이터가 정상적으로 탑재되었는지 확인
        assertThat(cache.get(10)).isNotNull();
        @SuppressWarnings("unchecked")
        List<PopularProductResponse> cachedValue = (List<PopularProductResponse>) cache.get(10).get();
        assertThat(cachedValue).isEqualTo(firstCall);

        // 4. 상품의 판매량을 임의로 변경합니다. (하지만 캐시로 인해 결과는 바뀌지 않아야 함)
        productB.increaseSalesCount(200); // B 판매량을 90 -> 290개로 대폭 늘려 B가 더 인기가 많아지게 함
        productRepository.saveAndFlush(productB);

        // 5. 두 번째 호출 (DB를 안 타고 캐시에서 꺼내오므로, 데이터가 여전히 이전 캐시값 [A, B] 순서여야 함)
        List<PopularProductResponse> secondCall = productService.getPopularProducts(10);
        
        assertThat(secondCall.get(0).productId()).isEqualTo(productA.getId()); // 여전히 A가 1등 (캐싱된 데이터가 반환되었음)
        assertThat(secondCall).isEqualTo(firstCall); // 최초 응답 데이터와 완전히 일치하는지 검증
    }
}
