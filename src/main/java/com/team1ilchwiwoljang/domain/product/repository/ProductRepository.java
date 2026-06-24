package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByStatus(ProductStatus status, Sort sort);
    List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Sort sort);
}
