package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatRoomUpdateStatusRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomListResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final ChatMessageService chatMessageService;

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

    /**
     * 관리자가 고객 채팅방 목록을 조회합니다.
     * status 쿼리 파라미터가 없으면 전체 조회,
     * status 쿼리 파라미터가 있으면 상태별 조회 합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomListResponse>>> getAllChatRooms(
            @Auth AuthMember authMember,
            @RequestParam(required = false) ChatRoomStatus status
    ) {
        List<ChatRoomListResponse> response =
                chatRoomService.getChatRooms(authMember.role(), status);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 채팅방의 메시지를 조회합니다.
     * MEMBER는 본인 채팅방 메시지만 조회할 수 있습니다.
     * ADMIN은 모든 채팅방 메시지를 조회할 수 있습니다.
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @Auth AuthMember authMember,
            @PathVariable Long chatRoomId
    ) {
        List<ChatMessageResponse> response =
                chatMessageService.getMessages(
                        authMember.memberId(),
                        authMember.role(),
                        chatRoomId
                );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 관리자가 고객 채팅방의 상담 상태를 변경합니다.
     * 일반 회원이 호출하면 FORBIDDEN 예외가 발생합니다.
     * 잘못된 상태 전이이면 INVALID_CHAT_ROOM_STATUS_TRANSITION 예외가 발생합니다.
     */
    @PatchMapping("/{chatRoomId}/status")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> changeChatRoomStatus(
            @Auth AuthMember authMember,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomUpdateStatusRequest request
    ) {
        ChatRoom chatRoom = chatRoomService.changeChatRoomStatus(
                authMember.role(),
                chatRoomId,
                request.status()
        );

        return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(chatRoom)));
    }
}
