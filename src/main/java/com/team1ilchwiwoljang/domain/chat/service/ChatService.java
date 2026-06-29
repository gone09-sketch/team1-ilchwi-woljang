package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberService memberService;

    @Transactional
    public ChatRoomResponse createChatRoom(Long memberId) {
        Member member = getMember(memberId);
        ChatRoom chatRoom = ChatRoom.create(member);
        chatRoomRepository.save(chatRoom);

        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        Member sender = getMember(senderId);
        ChatRoom chatRoom = getChatRoom(chatRoomId);

        validateRoomAccess(chatRoom, sender.getId(), sender.getRole());
        validateRoomOpen(chatRoom);

        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, request.content());
        chatMessageRepository.save(chatMessage);

        return ChatMessageResponse.from(chatMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long chatRoomId, Long memberId, MemberRole role) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        validateRoomAccess(chatRoom, memberId, role);

        return chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(chatRoomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateRoomAccess(Long chatRoomId, Long memberId, MemberRole role) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        validateRoomAccess(chatRoom, memberId, role);
    }

    private Member getMember(Long memberId) {
        return memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private ChatRoom getChatRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateRoomAccess(ChatRoom chatRoom, Long memberId, MemberRole role) {
        if (!chatRoom.isAccessibleBy(memberId, role)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private void validateRoomOpen(ChatRoom chatRoom) {
        if (chatRoom.isClosed()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_CLOSED);
        }
    }
}
