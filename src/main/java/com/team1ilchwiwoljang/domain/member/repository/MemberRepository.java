package com.team1ilchwiwoljang.domain.member.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
}
