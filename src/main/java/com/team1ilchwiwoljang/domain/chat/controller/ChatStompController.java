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
 * 흐름:
 * 1. 클라이언트가 /ws/chat 으로 STOMP 연결
 * 2. CONNECT frame에서 JWT 인증
 * 3. /sub/chat/rooms/{chatRoomId} 구독
 * 4. /pub/chat/rooms/{chatRoomId}/messages 로 메시지 발행
 * 5. 서버가 DB 저장 후 /sub/chat/rooms/{chatRoomId} 로 broadcast
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String MEMBER_ID_ATTRIBUTE = "memberId";
    private static final String ROLE_ATTRIBUTE = "role";
    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 특정 채팅방에 메시지를 보낼 때 호출됩니다.
     * 클라이언트 SEND destination:
     *   /pub/chat/rooms/{chatRoomId}/messages
     * 실제 Controller 매핑:
     *   /chat/rooms/{chatRoomId}/messages
     * /pub prefix는 WebSocketConfig에서 제거된 뒤 @MessageMapping으로 라우팅됩니다.
     */
    @MessageMapping("/chat/rooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        /*
         * senderId는 클라이언트 요청 body에서 받지 않습니다.
         * 발신자는 CONNECT 단계에서 JWT 검증 후
         * STOMP session attributes에 저장한 memberId, role만 신뢰합니다.
         */
        Long senderId = getMemberId(headerAccessor);
        MemberRole role = getRole(headerAccessor);

        ChatMessageResponse savedMessage = chatMessageService.saveMessage(
                senderId,
                role,
                chatRoomId,
                request.content()
        );

        /*
         * 저장된 메시지만 구독자에게 발행합니다.
         * 클라이언트가 받은 메시지와 DB에 저장된 메시지가 어긋나지 않습니다.
         */
        messagingTemplate.convertAndSend(
                CHAT_ROOM_SUB_PREFIX + chatRoomId,
                savedMessage
        );
    }

    private Long getMemberId(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(headerAccessor);
        Object memberId = sessionAttributes.get(MEMBER_ID_ATTRIBUTE);

        if (!(memberId instanceof Long)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (Long) memberId;
    }

    private MemberRole getRole(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(headerAccessor);
        Object role = sessionAttributes.get(ROLE_ATTRIBUTE);

        if (!(role instanceof MemberRole)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (MemberRole) role;
    }

    private Map<String, Object> getSessionAttributes(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return sessionAttributes;
    }
}