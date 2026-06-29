package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long chatRoomId,
        Long senderId,
        MemberRole senderRole,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSenderRole(),
                chatMessage.getContent(),
                chatMessage.getCreatedAt()
        );
    }
}
