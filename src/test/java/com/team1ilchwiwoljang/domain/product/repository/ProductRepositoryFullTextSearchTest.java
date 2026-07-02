package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * searchByNameFullText는 MATCH() AGAINST() 네이티브 쿼리를 쓰는데, 이건 MySQL 전용 문법이라 H2에서는 실행할 수 없습니다.
 * 그래서 기본 test profile(H2)에서는 이 테스트를 건너뛰고, 실제 MySQL에서만 실행되도록 조건을 걸었습니다.
 * OrderServiceIntegrationTest의 TestProfileResolver와 같은 방식으로 환경변수 spring.profiles.active=mysql일 때만 동작합니다.
 * 실행 방법: spring.profiles.active=mysql ./gradlew test
 * FULLTEXT 인덱스는 ProductFullTextIndexInitializer가 앱 기동 시 자동으로 생성해줍니다.
 *
 * products 테이블에 인덱싱 검증용 더미 데이터(100만 건)가 이미 있어서, deleteAll()로 정리하면
 * 전체 테이블을 메모리에 올리려다 OOM이 납니다. 그래서 이 테스트에서 만든 데이터만 골라서 지웁니다.
 */
@SpringBootTest
@ActiveProfiles("mysql-test")
@EnabledIfEnvironmentVariable(named = "spring.profiles.active", matches = "mysql")
class ProductRepositoryFullTextSearchTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void searchByNameFullTextFindsMatchingProductAndExcludesStopped() {
        Category category = categoryRepository.save(Category.createRoot("나이키후드집업테스트카테고리"));
        Product matched = productRepository.save(Product.create("나이키후드집업", 29_000, 10, ProductStatus.ON_SALE, "판매중", category));
        Product stopped = productRepository.save(Product.create("나이키후드집업중지", 9_000, 10, ProductStatus.STOPPED, "중지", category));
        Product other = productRepository.save(Product.create("아디다스바지", 39_000, 10, ProductStatus.ON_SALE, "바지", category));

        try {
            Page<Product> result = productRepository.searchByNameFullText(
                    "나이키",
                    ProductStatus.STOPPED.name(),
                    PageRequest.of(0, 20)
            );

            assertThat(result.getContent())
                    .extracting(Product::getName)
                    .containsExactly("나이키후드집업");
        } finally {
            productRepository.delete(matched);
            productRepository.delete(stopped);
            productRepository.delete(other);
            categoryRepository.delete(category);
        }
    }
}
