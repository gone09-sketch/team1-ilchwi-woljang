package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import java.util.List;

import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 메시지 저장과 조회를 담당하는 서비스입니다.
 * 핵심 권한 규칙:
 * 1. MEMBER는 본인의 채팅방 메시지만 조회할 수 있습니다.
 * 2. ADMIN은 모든 채팅방 메시지를 조회할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_MESSAGE_CONTENT_LENGTH = 1000;

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberService memberService;

    @Transactional(readOnly = true)
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

    /**
     * WebSocket으로 전달받은 채팅 메시지를 DB에 저장합니다.
     * 이 메서드는 단순히 메시지만 저장하지 않고,
     * 저장 전에 먼저 채팅방 접근 권한을 확인합니다.
     * MEMBER:
     * - 본인에게 할당된 채팅방에만 메시지를 저장할 수 있습니다.
     * ADMIN:
     * - 모든 회원 채팅방에 답변 메시지를 저장할 수 있습니다.
     */
    @Transactional
    public ChatMessageResponse saveMessage(
            Long senderId,
            MemberRole role,
            Long chatRoomId,
            String content
    ) {

        // 빈 메시지는 저장하지 않습니다.
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        /*
         * DB의 chat_messages.content 컬럼 길이가 1000자이므로
         * 저장 전에 서비스 계층에서 먼저 길이를 검증합니다.
         * 이렇게 하면 DB 예외가 그대로 노출되는 대신
         * 일관된 검증 실패 예외로 처리할 수 있습니다.
         */
        String trimmedContent = content.trim();

        if (trimmedContent.length() > MAX_MESSAGE_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        /*
         * MEMBER가 본인 채팅방이 아닌 chatRoomId로 메시지를 보내면 FORBIDDEN 예외가 발생합니다.
         * ADMIN은 모든 채팅방 접근이 허용됩니다.
         */
        ChatRoom chatRoom = chatRoomService.getAccessibleChatRoom(senderId, role, chatRoomId);

        /*
         * 메시지를 보낸 회원을 조회합니다.
         * findById는 탈퇴하지 않은 활성 회원만 조회하므로,
         * 삭제된 회원의 메시지 저장을 막을 수 있습니다.
         */
        Member sender = memberService.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.create(
                chatRoom,
                sender,
                trimmedContent
        );

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        return ChatMessageResponse.from(savedMessage);
    }
}
