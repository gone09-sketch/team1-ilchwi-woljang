package com.team1ilchwiwoljang.domain.chat.interceptor;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.JwtTokenPayload;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.auth.ChatPrincipal;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * 클라이언트에서 서버로 들어오는 STOMP frame을 가로채 인증/인가를 처리합니다.
 * 처리 범위:
 * - CONNECT: STOMP 연결 시점에 Access Token을 검증하고 ChatPrincipal을 설정합니다.
 * - SUBSCRIBE: 채팅방 구독 시점에 해당 채팅방 접근 권한을 검증합니다.
 * HTTP API 인증은 JwtAuthenticationFilter와 @Auth가 담당하지만,
 * STOMP frame은 HTTP Controller 요청이 아니므로 ChannelInterceptor에서 별도로 인증합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatStompChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /*
     * 채팅방 구독 destination prefix입니다.
     * 이 prefix로 시작하는 SUBSCRIBE 요청만 채팅방 접근 권한 검증 대상으로 봅니다.
     */
    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final ChatRoomService chatRoomService;

    /**
     * STOMP frame이 실제 메시지 처리 로직으로 전달되기 전에 실행됩니다.
     * CONNECT에서 인증된 Principal을 설정해두면 이후 SEND/SUBSCRIBE frame에서 같은 사용자를 식별할 수 있습니다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscribe(accessor);
        }

        /*
         * CONNECT에서 설정한 Principal이 이후 STOMP session에 남아야 하므로
         * 변경된 accessor headers를 담은 Message를 반환합니다.
         */
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    /**
     * STOMP CONNECT frame에서 Access Token을 검증하고 ChatPrincipal을 설정합니다.
     * 클라이언트는 CONNECT native header에 Authorization: Bearer {accessToken} 값을 담아 보냅니다.
     * 여기서 인증에 성공해야 이후 @MessageMapping 메서드와 STOMP event listener에서 Principal을 사용할 수 있습니다.
     */
    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
        JwtTokenPayload tokenPayload = jwtTokenProvider.parseAccessToken(accessToken);

        Long memberId = tokenPayload.memberId();
        MemberRole role = tokenPayload.role();

        /*
         * 토큰 자체가 유효하더라도 탈퇴했거나 비활성화된 회원이면 STOMP 연결을 허용하지 않습니다.
         */
        if (!memberService.existsActiveMember(memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        /*
         * STOMP session의 사용자 정보를 설정합니다.
         * 이후 메시지 발행(@MessageMapping)과 구독 이벤트에서는 session attributes가 아니라
         * 이 ChatPrincipal을 통해 발신자와 역할을 식별합니다.
         */
        accessor.setUser(new ChatPrincipal(memberId, role));
    }

    /**
     * STOMP SUBSCRIBE frame의 destination을 확인하고 채팅방 접근 권한을 검증합니다.
     */
    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHAT_ROOM_SUB_PREFIX)) {
            return;
        }

        ChatPrincipal principal = getChatPrincipal(accessor);
        Long chatRoomId = extractChatRoomId(destination);

        /*
         * MEMBER는 본인 채팅방만 구독할 수 있고,
         * ADMIN은 모든 회원 채팅방을 구독할 수 있습니다.
         */
        chatRoomService.getAccessibleChatRoom(
                principal.memberId(),
                principal.role(),
                chatRoomId
        );
    }

    /**
     * 채팅방 구독 destination에서 chatRoomId를 추출합니다.
     * destination은 /sub/chat/rooms/{chatRoomId} 형식이어야 합니다.
     */
    private Long extractChatRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(CHAT_ROOM_SUB_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private ChatPrincipal getChatPrincipal(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (!(principal instanceof ChatPrincipal chatPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return chatPrincipal;
    }
}
