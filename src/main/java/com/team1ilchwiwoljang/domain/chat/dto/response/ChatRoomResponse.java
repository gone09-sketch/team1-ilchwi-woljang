package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;

/**
 * 고객이 본인 채팅방을 조회할 때 내려주는 응답 DTO입니다.
 * 현재 채팅방의 상담 상태입니다.
 * WAITING: 상담 대기중
 * IN_PROGRESS: 상담 처리중
 * COMPLETED: 상담 완료
 */
public record ChatRoomResponse(
        Long chatRoomId,
        ChatRoomStatus status
) {

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getStatus()
        );
    }
}
