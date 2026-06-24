package com.team1ilchwiwoljang.domain.member.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMember(Long memberId){
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId);
    }

    public boolean existsActiveMember(Long memberId) {
        return memberRepository.existsByIdAndDeletedAtIsNull(memberId);
    }
}
