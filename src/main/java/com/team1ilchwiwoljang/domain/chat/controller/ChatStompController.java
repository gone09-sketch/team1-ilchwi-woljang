package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP 채팅 메시지 발행을 처리하는 Controller입니다.
 * 이 클래스는 HTTP 요청을 처리하는 @RestController가 아니라,
 * STOMP SEND frame을 처리하는 메시지 Controller입니다.
 * 현재 WebSocketConfig 기준 destination 설계:
 * 1. WebSocket 연결 endpoint
 *    - /ws/chat
 * 2. 클라이언트 SEND
 *    - /pub/chat/rooms/{chatRoomId}/messages
 * 3. Controller @MessageMapping 처리 경로
 *    - /chat/rooms/{chatRoomId}/messages
 * 4. 서버 broadcast
 *    - /sub/chat/rooms/{chatRoomId}
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String MEMBER_ID_ATTRIBUTE = "memberId";
    private static final String ROLE_ATTRIBUTE = "role";
    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final ChatMessageService chatMessageService;

    /*
     * 특정 STOMP destination으로 메시지를 발행할 때 사용하는 Spring Messaging 컴포넌트입니다.
     * convertAndSend(destination, payload)를 호출하면
     * 해당 destination을 구독 중인 클라이언트에게 payload가 전달됩니다.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 특정 채팅방으로 메시지를 보낼 때 호출됩니다.
     * 처리 흐름:
     * 1. destination path에서 chatRoomId를 꺼냅니다.
     * 2. STOMP session attributes에서 인증된 memberId, role을 꺼냅니다.
     * 3. ChatMessageService.saveMessage()로 권한 검증 후 DB에 저장합니다.
     * 4. 저장된 메시지를 /sub/chat/rooms/{chatRoomId} 로 발행합니다.
     * 5. 해당 destination을 구독 중인 회원/관리자가 실시간 메시지를 수신합니다.
     */
    @MessageMapping("/chat/rooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload ChatMessageRequest request,

            /*
             * STOMP session attributes에 접근하기 위한 객체입니다.
             * ChatStompChannelInterceptor가 CONNECT 단계에서 저장해둔
             * memberId, role을 여기서 꺼내 사용합니다.
             */
            SimpMessageHeaderAccessor headerAccessor
    ) {
        /*
         * 클라이언트가 body에 senderId를 보내더라도 신뢰하지 않습니다.
         * 발신자는 반드시 서버가 CONNECT 단계에서 인증한 session attributes 기준으로 식별하여
         * 일반 회원이 관리자처럼 sender 값을 조작하는 문제를 방어합니다.
         */
        Long senderId = getMemberId(headerAccessor);
        MemberRole role = getRole(headerAccessor);

        // saveMessage 내부에서 다시 한 번 채팅방 접근 권한을 검증합니다.
        ChatMessageResponse savedMessage = chatMessageService.saveMessage(
                senderId,
                role,
                chatRoomId,
                request.content()
        );

        // 채팅방별 destination으로 메시지를 발행합니다.
        messagingTemplate.convertAndSend(
                CHAT_ROOM_SUB_PREFIX + chatRoomId,
                savedMessage
        );
    }

    /**
     * STOMP session attributes에서 인증된 memberId를 꺼냅니다.
     * memberId는 ChatStompChannelInterceptor가 CONNECT 단계에서 JWT를 검증한 뒤 저장합니다.
     * 이 값이 없다는 것은 정상적인 STOMP 인증 과정을 거치지 않았다는 의미입니다.
     */
    private Long getMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(headerAccessor);

        Object memberId = sessionAttributes.get(MEMBER_ID_ATTRIBUTE);

        if (!(memberId instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (Long) memberId;
    }

    /**
     * STOMP session attributes에서 인증된 사용자의 role을 꺼냅니다.
     * role은 ChatStompChannelInterceptor가 CONNECT 단계에서 JWT를 검증한 뒤 저장합니다.
     * 이 값이 없으면 MEMBER인지 ADMIN인지 판단할 수 없으므로 인가 처리를 진행할 수 없습니다.
     */
    private MemberRole getRole(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(headerAccessor);

        Object role = sessionAttributes.get(ROLE_ATTRIBUTE);

        if (!(role instanceof MemberRole)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (MemberRole) role;
    }

    /**
     * STOMP session attributes를 안전하게 꺼냅니다.
     * headerAccessor.getSessionAttributes()가 null이면
     * CONNECT 단계에서 인증 정보가 저장되지 않았거나,
     * 현재 메시지가 정상적인 STOMP session 흐름 안에서 처리되지 않은 것입니다.
     */
    private Map<String, Object> getSessionAttributes(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return sessionAttributes;
    }
}