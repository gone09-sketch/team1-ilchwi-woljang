package com.team1ilchwiwoljang.domain.auth.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.auth.dto.request.SignupRequest;
import com.team1ilchwiwoljang.domain.auth.dto.response.SignupResponse;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(request.email(), encodedPassword, request.name(), request.phone());

        Member savedMember = memberRepository.save(member);

        return SignupResponse.from(savedMember);
    }
}
