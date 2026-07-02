package com.team1ilchwiwoljang.domain.chat.listener;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatSystemMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

/**
 * STOMP SUBSCRIBE가 완료된 뒤 실행되는 이벤트 Listener입니다.
 * ChatStompChannelInterceptor는 SUBSCRIBE 권한 검증만 담당하고,
 * 이 Listener는 구독이 승인된 채팅방에 시스템 메시지를 발행합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatStompSubscribeEventListener {

    private static final String ROLE_ATTRIBUTE = "role";
    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        // 채팅방 구독 destination이 아니면 시스템 메시지 대상이 아닙니다.
        if (destination == null || !destination.startsWith(CHAT_ROOM_SUB_PREFIX)) {
            return;
        }

        Long chatRoomId = extractChatRoomId(destination);
        MemberRole role = getRole(accessor);

        // 이미 실제 채팅 메시지가 있다면 재입장으로 보고 시스템 메시지를 보내지 않습니다.
        if (chatMessageService.hasMessages(chatRoomId)) {
            return;
        }

        // STOMP에서는 서버가 destination에 발행하면 Simple Broker가 구독자에게 전달합니다.
        messagingTemplate.convertAndSend(
                destination,
                ChatSystemMessageResponse.entered(chatRoomId, role)
        );
    }

    private Long extractChatRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(CHAT_ROOM_SUB_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private MemberRole getRole(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object role = sessionAttributes.get(ROLE_ATTRIBUTE);

        if (!(role instanceof MemberRole)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return (MemberRole) role;
    }
}