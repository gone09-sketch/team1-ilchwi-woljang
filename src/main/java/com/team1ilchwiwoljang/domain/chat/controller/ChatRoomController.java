package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomListResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 생성과 조회를 담당하는 Controller입니다.
 * 순수 WebSocket 연결 자체는 /ws/chat에서 처리하지만,
 * WebSocket 연결 전에 회원에게 할당된 chatRoomId를 알아야 하므로
 * HTTP API로 채팅방을 먼저 생성하거나 조회합니다.
 */
@Validated
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
     * 관리자가 모든 회원 채팅방 목록을 조회합니다.
     * MEMBER가 호출하면 ChatRoomService에서 FORBIDDEN 예외가 발생합니다.
     * 채팅방은 계속 늘어나는 데이터이므로 page/size로 한 번에 조회할 양을 제한합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChatRoomListResponse>>> getAllChatRooms(
            @Auth AuthMember authMember,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ChatRoomListResponse> response =
                chatRoomService.getAllChatRooms(authMember.role(), page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 채팅방의 메시지를 조회합니다.
     * MEMBER는 본인 채팅방 메시지만 조회할 수 있습니다.
     * ADMIN은 모든 채팅방 메시지를 조회할 수 있습니다.
     * 메시지는 누적 데이터이므로 page/size로 한 번에 조회할 양을 제한합니다.
     */
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageResponse>>> getMessages(
            @Auth AuthMember authMember,
            @Min(1) @PathVariable Long chatRoomId,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ChatMessageResponse> response =
                chatMessageService.getMessages(
                        authMember.memberId(),
                        authMember.role(),
                        chatRoomId,
                        page,
                        size
                );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
