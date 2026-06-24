package com.team1ilchwiwoljang.domain.auth.service;

import com.team1ilchwiwoljang.domain.auth.entity.RefreshToken;
import com.team1ilchwiwoljang.domain.auth.repository.RefreshTokenRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Refresh Token을 폐기하면 revoked 값이 true로 저장된다")
    void givenRefreshToken_whenRevoke_thenMarkTokenAsRevoked() {
        // given
        Member member = memberRepository.save(
                Member.create("test@example.com", "encoded-password", "테스트", "01012345678")
        );
        String refreshTokenValue = "refresh-token";
        RefreshToken refreshToken = refreshTokenRepository.save(
                RefreshToken.createRefreshToken(member, refreshTokenValue, Instant.now().plusSeconds(60))
        );

        // when
        refreshTokenService.revoke(refreshTokenValue);

        // then
        RefreshToken savedToken = refreshTokenRepository.findById(refreshToken.getId())
                .orElseThrow();

        assertThat(savedToken.isRevoked()).isTrue();
    }
}
