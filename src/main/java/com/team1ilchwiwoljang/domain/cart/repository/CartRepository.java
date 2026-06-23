package com.team1ilchwiwoljang.domain.cart.repository;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
            select c
            from Cart c
            join fetch c.product
            where c.member.id = :memberId
            order by c.id desc
            """)
    List<Cart> findAllByMemberIdWithProduct(@Param("memberId") Long memberId);

    Optional<Cart> findByMemberIdAndProductId(Long memberId, Long productId);


}
