package com.team1ilchwiwoljang.domain.chat.interceptor;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.JwtTokenPayload;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * STOMP inbound message를 가로채서 인증/인가를 처리하는 Interceptor입니다.
 * 처리 범위:
 * 1. CONNECT
 *    - STOMP 연결 시점에 Authorization header의 Access Token을 검증합니다.
 *    - 검증된 memberId, role을 STOMP session attributes에 저장합니다.
 * 2. SUBSCRIBE
 *    - 클라이언트가 특정 채팅방 sub을 구독할 때 권한을 검증합니다.
 *    - MEMBER는 본인 채팅방 sub만 구독할 수 있습니다.
 *    - ADMIN은 모든 회원 채팅방 sub을 구독할 수 있습니다.
 */
@Component
@RequiredArgsConstructor
public class ChatStompChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MEMBER_ID_ATTRIBUTE = "memberId";
    private static final String ROLE_ATTRIBUTE = "role";

    /*
     * 채팅방 구독 destination prefix입니다.
     * 이 prefix로 시작하는 SUBSCRIBE 요청만 채팅방 접근 권한 검증 대상으로 봅니다.
     */
    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final ChatRoomService chatRoomService;

    /**
     * 클라이언트가 서버로 보내는 STOMP frame이 실제 메시지 처리 로직으로 전달되기 전에 실행됩니다.
     * 이 메서드에서는 STOMP command를 확인한 뒤,
     * CONNECT와 SUBSCRIBE에 대해 필요한 인증/인가 로직을 수행합니다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        /*
         * Message<?>는 Spring Messaging에서 사용하는 추상 메시지 객체입니다.
         * 이 객체 안에는 STOMP command, destination, native header, session attributes 같은 정보가 들어 있습니다.
         * 하지만 직접 다루기 불편하기 때문에 StompHeaderAccessor로 감싸서 사용합니다.
         */
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        /*
         * CONNECT 프레임:
         * STOMP 연결을 처음 맺을 때 들어옵니다.
         * 여기서 JWT를 검증하고, 이후 SEND/SUBSCRIBE에서 사용할 인증 정보를 세션에 저장합니다.
         */
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        /*
         * SUBSCRIBE 프레임:
         * 클라이언트가 특정 destination을 구독할 때 들어옵니다.
         */
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscribe(accessor);
        }

        /*
         * 인증/인가 검증을 통과한 메시지만 다음 처리 단계로 전달합니다.
         * 중간에 BusinessException이 발생하면 message는 더 이상 정상 처리되지 않고,
         * STOMP 연결 또는 구독 요청이 실패하게 됩니다.
         */
        return message;
    }

    /**
     * STOMP CONNECT frame에서 Access Token을 추출하고,
     * 인증된 사용자 정보를 STOMP session attributes에 저장합니다.
     * CONNECT는 WebSocket 연결 이후 STOMP 프로토콜 레벨에서 처음 보내는 연결 요청입니다.
     * 이 시점에 사용자를 인증해두어야 이후 SUBSCRIBE, SEND 요청에서 같은 사용자의 요청인지 판단할 수 있습니다.
     */
    private void authenticateConnect(StompHeaderAccessor accessor) {
        /*
         * STOMP native header에서 Authorization 값을 꺼냅니다.
         * HTTP API에서는 request header에서 Authorization을 읽지만,
         * STOMP에서는 CONNECT frame의 native header에 Authorization 값을 담아 보냅니다.
         */
        String authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        /*
         * Authorization header가 없거나 Bearer 형식이 아니면 인증 실패로 처리합니다.
         * 이 검증이 없으면 토큰 없이도 STOMP 연결을 시도할 수 있기 때문에 CONNECT 단계에서 반드시 차단합니다.
         */
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // "Bearer " prefix를 제거하고 순수 Access Token 값만 추출합니다.
        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

        // Access Token을 검증하고 payload를 추출합니다.
        JwtTokenPayload tokenPayload = jwtTokenProvider.parseAccessToken(accessToken);


        // JWT payload에서 인증된 회원 ID와 권한을 꺼냅니다.
        Long memberId = tokenPayload.memberId();
        MemberRole role = tokenPayload.role();

        // 토큰이 유효해도 회원이 탈퇴했거나 존재하지 않으면 연결을 거부합니다.
        if (!memberService.existsActiveMember(memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        /*
         * STOMP session attributes에 인증 정보를 저장합니다.
         * 이후 Controller나 SUBSCRIBE 검증에서 이 값을 꺼내 사용합니다.
         */
        accessor.getSessionAttributes().put(MEMBER_ID_ATTRIBUTE, memberId);
        accessor.getSessionAttributes().put(ROLE_ATTRIBUTE, role);
    }

    /**
     * STOMP SUBSCRIBE frame의 destination을 확인하고 채팅방 접근 권한을 검증합니다.
     * SUBSCRIBE는 클라이언트가 특정 destination의 메시지를 받겠다고 서버에 요청하는 단계입니다.
     */
    private void validateSubscribe(StompHeaderAccessor accessor) {
        // 클라이언트가 구독하려는 destination입니다.
        String destination = accessor.getDestination();

        // 채팅방 sub이 아닌 destination은 여기서 검증하지 않습니다.
        if (destination == null || !destination.startsWith(CHAT_ROOM_SUB_PREFIX)) {
            return;
        }

        /*
         * CONNECT 단계에서 session attributes에 저장해둔 인증 정보를 꺼냅니다.
         * 이 값이 없다는 것은 정상적인 CONNECT 인증 과정을 거치지 않았거나,
         * session 상태가 올바르지 않다는 의미이므로 UNAUTHORIZED로 처리합니다.
         */
        Long memberId = getMemberId(accessor);
        MemberRole role = getRole(accessor);

        // destination 문자열에서 chatRoomId를 추출합니다.
        Long chatRoomId = extractChatRoomId(destination);

        /*
         * MEMBER는 본인 채팅방만 통과합니다.
         * ADMIN은 모든 채팅방을 통과합니다.
         */
        chatRoomService.getAccessibleChatRoom(memberId, role, chatRoomId);
    }

    /**
     * 채팅방 구독 destination에서 chatRoomId를 추출합니다.
     * destination은 /sub/chat/rooms/{chatRoomId} 형식이어야 합니다.
     */
    private Long extractChatRoomId(String destination) {
        try {
            // prefix 뒤에 있는 값만 잘라 chatRoomId 문자열로 사용합니다.
            String chatRoomIdValue = destination.substring(CHAT_ROOM_SUB_PREFIX.length());

            // URL path에서 추출한 값은 문자열이므로 Long 타입으로 변환합니다.
            return Long.valueOf(chatRoomIdValue);
        } catch (NumberFormatException e) {
            /*
             * chatRoomId 자리에 숫자가 아닌 값이 들어온 경우입니다.
             * 이 경우 채팅방 조회 자체를 진행하지 않고 요청 검증 실패로 처리합니다.
             */
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    /**
     * STOMP session attributes에서 인증된 memberId를 꺼냅니다.
     * memberId는 CONNECT 단계에서 JWT 검증 후 저장됩니다.
     */
    private Long getMemberId(StompHeaderAccessor accessor) {
        // CONNECT 단계에서 저장한 memberId 값을 session attributes에서 조회합니다.
        Object memberId = accessor.getSessionAttributes().get(MEMBER_ID_ATTRIBUTE);

        // memberId가 없거나 Long 타입이 아니면 인증되지 않은 요청으로 판단합니다.
        if (!(memberId instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 타입 검증을 통과했으므로 Long으로 캐스팅해 반환합니다.
        return (Long) memberId;
    }

    /**
     * STOMP session attributes에서 인증된 사용자의 role을 꺼냅니다.
     * role은 CONNECT 단계에서 JWT 검증 후 저장됩니다.
     * 이 값이 없으면 MEMBER인지 ADMIN인지 판단할 수 없으므로 인가 처리를 진행할 수 없습니다.
     */
    private MemberRole getRole(StompHeaderAccessor accessor) {
        // CONNECT 단계에서 저장한 role 값을 session attributes에서 조회합니다.
        Object role = accessor.getSessionAttributes().get(ROLE_ATTRIBUTE);

        // role이 없거나 MemberRole 타입이 아니면 인증되지 않은 요청으로 판단합니다.
        if (!(role instanceof MemberRole)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (MemberRole) role;
    }
}