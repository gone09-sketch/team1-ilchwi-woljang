package com.team1ilchwiwoljang.domain.chat.auth;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;

import java.security.Principal;

/**
 * STOMP 연결에서 인증된 사용자를 표현하는 Principal입니다.
 *
 * HTTP API는 Spring Security Filter가 만든 AuthMember를 @Auth로 주입받지만,
 * STOMP 메시지는 CONNECT 이후의 frame을 ChannelInterceptor에서 직접 다룹니다.
 * 그래서 STOMP 전용 Principal을 만들어 CONNECT 인증 결과를 WebSocket session user로 저장합니다.
 */
public record ChatPrincipal(
        Long memberId,
        MemberRole role
) implements Principal {

    /**
     * Spring Messaging이 사용자 식별자로 사용하는 값입니다.
     * 현재 채팅 도메인에서는 memberId가 사용자 고유 식별자이므로 문자열로 변환해 반환합니다.
     */
    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
