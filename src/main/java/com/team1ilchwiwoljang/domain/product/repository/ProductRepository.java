package com.team1ilchwiwoljang.domain.product.repository;

import com.team1ilchwiwoljang.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
