package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 생성과 조회를 담당하는 Controller입니다.
 * 순수 WebSocket 연결 자체는 /ws/chat에서 처리하지만,
 * WebSocket 연결 전에 회원에게 할당된 chatRoomId를 알아야 하므로
 * HTTP API로 채팅방을 먼저 생성하거나 조회합니다.
 */
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 로그인한 회원 본인에게 채팅방을 생성합니다.
     */
    @PostMapping("/my")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createMyChatRoom(
            @Auth AuthMember authMember
    ) {
        ChatRoom chatRoom = chatRoomService.createMyChatRoom(authMember.memberId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(ChatRoomResponse.from(chatRoom)));
    }

    /**
     * 로그인한 회원 본인에게 이미 할당된 채팅방을 조회합니다.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getMyChatRoom(
            @Auth AuthMember authMember
    ) {
        ChatRoom chatRoom = chatRoomService.getMyChatRoom(authMember.memberId());

        return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(chatRoom)));
    }
}
