package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import java.time.LocalDateTime;

/**
 * 관리자가 전체 채팅방 목록을 조회할 때 사용하는 응답 DTO입니다.
 */
public record ChatRoomListResponse(
        Long chatRoomId,
        Long memberId,
        LocalDateTime createdAt
) {

    public static ChatRoomListResponse from(ChatRoom chatRoom) {
        return new ChatRoomListResponse(
                chatRoom.getId(),
                chatRoom.getMember().getId(),
                chatRoom.getCreatedAt()
        );
    }
}