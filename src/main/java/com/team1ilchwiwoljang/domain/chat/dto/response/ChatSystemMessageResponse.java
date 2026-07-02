package com.team1ilchwiwoljang.domain.chat.dto.response;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import java.time.LocalDateTime;

/**
 * STOMP로만 실시간 전달되는 시스템 메시지 응답 DTO입니다.
 * 일반 채팅 메시지는 ChatMessage 엔티티로 DB에 저장하지만,
 * 입장 안내 같은 시스템 메시지는 실시간 이벤트이므로 DB에 저장하지 않습니다.
 */
public record ChatSystemMessageResponse(
        // 프론트에서 일반 채팅 메시지와 시스템 메시지를 구분하기 위한 값입니다.
        String type,

        Long chatRoomId,

        // 화면에 표시할 시스템 메시지 내용입니다.
        String content,

        LocalDateTime createdAt
) {

    /**
     * 채팅방 구독, 즉 입장 시점에 보낼 시스템 메시지를 생성합니다.
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