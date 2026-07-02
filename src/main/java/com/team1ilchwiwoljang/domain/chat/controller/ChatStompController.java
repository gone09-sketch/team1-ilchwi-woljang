package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.auth.ChatPrincipal;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP 채팅 메시지 발행을 처리하는 Controller입니다.
 *
 * 흐름:
 * 1. 클라이언트가 /ws/chat 으로 STOMP 연결
 * 2. CONNECT frame에서 JWT 인증 후 ChatPrincipal 설정
 * 3. /sub/chat/rooms/{chatRoomId} 구독
 * 4. /pub/chat/rooms/{chatRoomId}/messages 로 메시지 발행
 * 5. 서버가 DB 저장 후 /sub/chat/rooms/{chatRoomId} 로 broadcast
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 특정 채팅방에 메시지를 보낼 때 호출됩니다.
     *
     * 클라이언트 SEND destination:
     *   /pub/chat/rooms/{chatRoomId}/messages
     *
     * 실제 Controller 매핑:
     *   /chat/rooms/{chatRoomId}/messages
     *
     * /pub prefix는 WebSocketConfig에서 제거된 뒤 @MessageMapping으로 라우팅됩니다.
     */
    @MessageMapping("/chat/rooms/{chatRoomId}/messages")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload ChatMessageRequest request,
            Principal principal
    ) {
        /*
         * senderId는 클라이언트 요청 body에서 받지 않습니다.
         * CONNECT 단계에서 JWT 검증 후 설정한 ChatPrincipal만 신뢰합니다.
         */
        ChatPrincipal chatPrincipal = getChatPrincipal(principal);

        ChatMessageResponse savedMessage = chatMessageService.saveMessage(
                chatPrincipal.memberId(),
                chatPrincipal.role(),
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

    private ChatPrincipal getChatPrincipal(Principal principal) {
        if (!(principal instanceof ChatPrincipal chatPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return chatPrincipal;
    }
}
