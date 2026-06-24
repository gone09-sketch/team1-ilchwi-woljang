package com.team1ilchwiwoljang.domain.member.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.auth.service.RefreshTokenService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
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
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public Member getMember(Long memberId){
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Optional<Member> findByEmailAndDeletedAtIsNull(String email) {
        return memberRepository.findByEmailAndDeletedAtIsNull(email);
    }

    public Optional<Member> findById(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId);
    }

    public boolean existsActiveMember(Long memberId) {
        return memberRepository.existsByIdAndDeletedAtIsNull(memberId);
    }

    @Transactional
    public void changeRole(Long memberId, MemberRole role) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() == role) {
            return;
        }

        member.changeRole(role);

        // role이 바뀌면 기존 Refresh Token을 모두 폐기합니다.
        refreshTokenService.revokeAllByMember(member);
    }
}
