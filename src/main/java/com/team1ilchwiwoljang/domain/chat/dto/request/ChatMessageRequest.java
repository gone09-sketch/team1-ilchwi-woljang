package com.team1ilchwiwoljang.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * STOMP로 채팅 메시지를 보낼 때 사용하는 요청 DTO입니다.
 * 클라이언트 SEND 예시:
 * destination:/pub/chat/rooms/1/messages
 * body:
 * {
 *   "content": "안녕하세요"
 * }
 * chatRoomId는 body에 넣지 않고 destination path에서 받습니다.
 * 그래야 메시지가 어느 채팅방으로 발행되는지 destination만 보고도 명확해집니다.
 */
public record ChatMessageRequest(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 1000, message = "메시지는 1000자 이하로 입력해야 합니다.")
        String content
) {
}