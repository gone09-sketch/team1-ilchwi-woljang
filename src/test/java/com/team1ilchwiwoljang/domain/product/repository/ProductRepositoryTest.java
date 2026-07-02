package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.common.config.QueryDslConfig;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.category.repository.CategoryRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByNameContainingIgnoreCaseAndStatusNotSearchesNameAndExcludesStopped() {
        Category category = categoryRepository.save(Category.createRoot("상의"));
        productRepository.save(Product.create("Basic Shirt", 29_000, 10, ProductStatus.ON_SALE, "판매중", category));
        productRepository.save(Product.create("basic shirt out", 19_000, 0, ProductStatus.OUT_OF_STOCK, "품절", category));
        productRepository.save(Product.create("Basic Shirt stopped", 9_000, 10, ProductStatus.STOPPED, "중지", category));
        productRepository.save(Product.create("Denim Pants", 39_000, 10, ProductStatus.ON_SALE, "바지", category));

        Page<Product> products = productRepository.findByNameContainingIgnoreCaseAndStatusNot(
                "BASIC SHIRT",
                ProductStatus.STOPPED,
                PageRequest.of(0, 20)
        );

        assertThat(products.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Basic Shirt", "basic shirt out");
        assertThat(products.getContent())
                .extracting(Product::getStatus)
                .contains(ProductStatus.ON_SALE, ProductStatus.OUT_OF_STOCK)
                .doesNotContain(ProductStatus.STOPPED);
    }
}
