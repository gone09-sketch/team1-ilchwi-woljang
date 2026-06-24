package com.team1ilchwiwoljang.domain.auth.repository;

import com.team1ilchwiwoljang.domain.auth.entity.RefreshToken;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    void deleteByMember(Member member);
}
