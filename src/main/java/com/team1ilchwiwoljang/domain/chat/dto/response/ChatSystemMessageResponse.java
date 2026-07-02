package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;

import java.time.LocalDateTime;

/**
 * WebSocket으로만 전달되는 시스템 메시지 응답 DTO입니다.
 * 일반 채팅 메시지는 ChatMessage 엔티티로 DB에 저장되지만,
 * 입장 안내 같은 시스템 메시지는 실시간 이벤트에 가깝습니다.
 * 따라서 DB에 저장하지 않고 WebSocket으로만 전달합니다.
 */
public record ChatSystemMessageResponse(
        // 프론트에서 일반 채팅 메시지와 시스템 메시지를 구분하기 위한 타입입니다.
        String type,

        Long chatRoomId,

        // 화면에 표시할 시스템 메시지 내용입니다.
        String content,

        LocalDateTime createdAt
) {

    /**
     * 고객 또는 관리자가 채팅방에 입장했을 때 사용할 시스템 메시지를 생성합니다.
     */
    public static ChatSystemMessageResponse entered(Long chatRoomId, MemberRole role) {
        String content = role == MemberRole.ADMIN
                ? "관리자가 채팅방에 입장했습니다."
                : "채팅방에 입장했습니다.";

        return new ChatSystemMessageResponse(
                "SYSTEM",
                chatRoomId,
                content,
                LocalDateTime.now()
        );
    }
}