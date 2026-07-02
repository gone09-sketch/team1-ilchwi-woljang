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
            where c.id = :cartItemId
              and c.member.id = :memberId
            """)
    Optional<Cart> findByIdAndMemberIdWithProduct(
            @Param("cartItemId") Long cartItemId,
            @Param("memberId") Long memberId
    );

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

    /**
     * 주문 생성 시 비관적 락 우회 방지용 쿼리.
     * Product를 fetch하지 않아 1차 캐시에 락 없는 Product가 올라오지 않습니다.
     * Product는 OrderService에서 비관적 락으로 별도 조회합니다.
     */
    @Query("""
            select c
            from Cart c
            where c.member.id = :memberId
              and c.id in :cartIds
            order by c.id desc
            """)
    List<Cart> findAllByMemberIdAndIdInWithoutProduct(
            @Param("memberId") Long memberId,
            @Param("cartIds") List<Long> cartIds
    );

}
