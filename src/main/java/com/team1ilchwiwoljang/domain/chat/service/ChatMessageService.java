package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 메시지 조회를 담당하는 서비스입니다.
 * 핵심 권한 규칙:
 * 1. MEMBER는 본인의 채팅방 메시지만 조회할 수 있습니다.
 * 2. ADMIN은 모든 채팅방 메시지를 조회할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessageResponse> getMessages(Long memberId, MemberRole role, Long chatRoomId) {
        /*
         * MEMBER가 본인 채팅방이 아닌 chatRoomId로 접근하면 FORBIDDEN 예외를 던집니다.
         * ADMIN은 모든 채팅방 접근을 허용합니다.
         */
        ChatRoom chatRoom = chatRoomService.getAccessibleChatRoom(memberId, role, chatRoomId);

        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(chatRoom.getId())
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }
}