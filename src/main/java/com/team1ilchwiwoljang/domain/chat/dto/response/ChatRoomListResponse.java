package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;

import java.time.LocalDateTime;

/**
 * 관리자가 전체 채팅방 목록을 조회할 때 사용하는 응답 DTO입니다.
 * 현재 채팅방의 상담 상태입니다.
 * WAITING: 상담 대기중
 * IN_PROGRESS: 상담 처리중
 * COMPLETED: 상담 완료
 */
public record ChatRoomListResponse(
        Long chatRoomId,
        Long memberId,
        ChatRoomStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChatRoomListResponse from(ChatRoom chatRoom) {
        return new ChatRoomListResponse(
                chatRoom.getId(),
                chatRoom.getMember().getId(),
                chatRoom.getStatus(),
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt()
        );
    }
}