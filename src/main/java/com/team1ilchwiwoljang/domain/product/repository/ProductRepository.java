package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndPriceBetween(
            ProductStatus status,
            int minPrice,
            int maxPrice,
            Pageable pageable);

    Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, ProductStatus status, Pageable pageable);

    // Spring Data 파생 쿼리로 간단히 유지합니다.
    // 검색 조건이 더 늘어나면 @Query 또는 Querydsl로 옮깁니다.
    Page<Product> findByNameContainingIgnoreCaseAndStatusNot(
            String keyword,
            ProductStatus excludedStatus,
            Pageable pageable
    );

    @Query(
            value = "SELECT * FROM products WHERE MATCH(name) AGAINST (:keyword IN NATURAL LANGUAGE MODE) AND status <> :excludedStatus ORDER BY MATCH(name) AGAINST (:keyword IN NATURAL LANGUAGE MODE) DESC",
            countQuery = "SELECT count(*) FROM products WHERE MATCH(name) AGAINST (:keyword IN NATURAL LANGUAGE MODE) AND status <> :excludedStatus",
            nativeQuery = true
    )
    // excludedStatus는 nativeQuery라 ProductStatus enum을 직접 바인딩할 수 없어 String으로 받습니다.
    // 호출부에서 ProductStatus.STOPPED.name()으로 넘겨야 하며, enum 이름이 바뀌면 여기도 같이 확인이 필요합니다.
    Page<Product> searchByNameFullText(
            @Param("keyword") String keyword,
            @Param("excludedStatus") String excludedStatus,
            Pageable pageable
    );

    List<Product> findByStatusOrderBySalesCountDesc(ProductStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.cache.retrieveMode", value = "BYPASS"),
            @QueryHint(name = "jakarta.persistence.cache.storeMode", value = "BYPASS")
    })
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByWithPessimisticLock(
            @Param("productId") Long productId
    );
}
