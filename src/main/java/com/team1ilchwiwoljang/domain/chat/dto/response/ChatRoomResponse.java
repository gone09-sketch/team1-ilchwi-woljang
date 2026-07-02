package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;

public record ChatRoomResponse(
        Long chatRoomId
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(chatRoom.getId());
    }
}
