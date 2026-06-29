package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        Long memberId,
        ChatRoomStatus status,
        LocalDateTime createdAt
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getMember().getId(),
                chatRoom.getStatus(),
                chatRoom.getCreatedAt()
        );
    }
}
