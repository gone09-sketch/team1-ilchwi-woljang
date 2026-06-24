package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    // Spring Data 파생 쿼리로 간단히 유지합니다.
    // 검색 조건이 더 늘어나면 @Query 또는 Querydsl로 옮깁니다.
    Page<Product> findByNameContainingIgnoreCaseAndStatusNot(
            String keyword,
            ProductStatus excludedStatus,
            Pageable pageable
    );
}
