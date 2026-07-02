package com.team1ilchwiwoljang.domain.chat.dto.request;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자가 채팅방 상담 상태를 변경할 때 사용하는 요청 DTO입니다.
 */
public record ChatRoomUpdateStatusRequest(
        @NotNull(message = "변경할 상담 상태는 필수입니다.")
        ChatRoomStatus status
) {
}