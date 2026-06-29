package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/rooms/{chatRoomId}/messages")
    public void send(
            @DestinationVariable Long chatRoomId,
            @Valid @Payload ChatMessageRequest request,
            Principal principal
    ) {
        AuthMember authMember = getAuthMember(principal);
        ChatMessageResponse response = chatService.sendMessage(chatRoomId, authMember.memberId(), request);

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + chatRoomId, response);
    }

    private AuthMember getAuthMember(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthMember authMember) {
            return authMember;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
