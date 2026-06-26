package com.team1ilchwiwoljang.domain.cart.repository;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByMemberIdAndProductId(Long memberId, Long productId);

    @Query("""
            select c
            from Cart c
            join fetch c.product
            where c.member.id = :memberId
            order by c.id desc
            """)
    List<Cart> findAllByMemberIdWithProduct(@Param("memberId") Long memberId);

    @Query("""
            select c
            from Cart c
            join fetch c.product
            where c.member.id = :memberId
              and c.id in :cartIds
            order by c.id desc
            """)
    List<Cart> findAllByMemberIdAndIdInWithProduct(
            @Param("memberId") Long memberId,
            @Param("cartIds") List<Long> cartIds
    );

}
