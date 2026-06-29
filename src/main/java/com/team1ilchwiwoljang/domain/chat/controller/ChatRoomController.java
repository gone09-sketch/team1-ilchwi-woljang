package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
            @Auth AuthMember authMember
    ) {
        ChatRoomResponse response = chatService.createChatRoom(authMember.memberId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @Auth AuthMember authMember,
            @PathVariable Long chatRoomId
    ) {
        List<ChatMessageResponse> response = chatService.getMessages(
                chatRoomId,
                authMember.memberId(),
                authMember.role()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
