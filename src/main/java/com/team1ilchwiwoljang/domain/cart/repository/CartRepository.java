package com.team1ilchwiwoljang.domain.cart.repository;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByMemberIdAndProductId(Long memberId, Long productId);


}
