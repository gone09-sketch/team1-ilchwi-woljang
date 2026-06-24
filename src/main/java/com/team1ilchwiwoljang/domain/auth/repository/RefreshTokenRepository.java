package com.team1ilchwiwoljang.domain.auth.repository;

import com.team1ilchwiwoljang.domain.auth.entity.RefreshToken;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.member.id = :memberId
              and rt.revoked = false
            """)
    int revokeAllByMemberId(@Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken rt
            set rt.revoked = true
            where rt.token = :token
              and rt.revoked = false
            """)
    int revokeByToken(@Param("token") String token);

    List<RefreshToken> findAllByMemberAndRevokedFalse(Member member);
}
