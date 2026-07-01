package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 생성, 조회, WebSocket 접속 권한 검증을 담당하는 서비스입니다.
 * 채팅방은 회원에게 1개씩 할당됩니다.
 * 회원은 본인에게 할당된 채팅방에만 접속할 수 있고,
 * 관리자는 모든 회원 채팅방에 접속할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberService memberService;

    /**
     * 회원 본인에게 할당된 채팅방을 조회합니다.
     */
    public ChatRoom getMyChatRoom(Long memberId) {
        return chatRoomRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    /**
     * 회원 본인에게 채팅방을 새로 할당합니다.
     * 현재 프로젝트의 일반 회원 권한은 MemberRole.MEMBER입니다.
     */
    @Transactional
    public ChatRoom createMyChatRoom(Long memberId) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() != MemberRole.MEMBER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (chatRoomRepository.existsByMember_Id(memberId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        return chatRoomRepository.save(ChatRoom.create(member));
    }

    /**
     * WebSocket 연결 전에 해당 회원 또는 관리자가 채팅방에 접속 가능한지 검증합니다.
     * 권한 규칙:
     * 1. ADMIN은 모든 회원 채팅방에 접속할 수 있습니다.
     * 2. MEMBER는 본인에게 할당된 채팅방에만 접속할 수 있습니다.
     * 3. 위 조건을 만족하지 않으면 접근을 거부합니다.
     */
    public ChatRoom getAccessibleChatRoom(Long memberId, MemberRole role, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (role == MemberRole.ADMIN) {
            return chatRoom;
        }

        if (role == MemberRole.MEMBER && chatRoom.isOwner(memberId)) {
            return chatRoom;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
