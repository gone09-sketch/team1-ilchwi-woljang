package com.team1ilchwiwoljang.domain.member.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    // 로그인 시, 탈퇴하지 않은 회원만 조회합니다.
    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(Long memberId);

    boolean existsByIdAndDeletedAtIsNull(Long memberId);
}
