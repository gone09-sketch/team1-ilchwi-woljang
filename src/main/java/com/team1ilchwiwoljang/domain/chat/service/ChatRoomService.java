package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomListResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        try {
            /*
             * existsByMember_Id 검사는 사용자에게 빠르게 중복을 알려주기 위한 1차 방어입니다.
             * 동시에 같은 회원이 생성 요청을 보내면 둘 다 exists 검사를 통과할 수 있으므로,
             * DB unique 제약 위반도 CHAT_ROOM_ALREADY_EXISTS로 변환합니다.
             */
            return chatRoomRepository.saveAndFlush(ChatRoom.create(member));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }
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

    /**
     * 관리자가 고객 채팅방 목록을 조회합니다.
     * status가 null이면 전체 채팅방을 조회하고,
     * status가 있으면 해당 상태의 채팅방만 조회합니다.
     * 일반 회원은 다른 고객의 채팅방 목록을 볼 수 없으므로
     * ADMIN이 아니면 FORBIDDEN 예외를 던집니다.
     */
    public List<ChatRoomListResponse> getChatRooms(MemberRole role, ChatRoomStatus status) {
        if (role != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        /*
         * status가 없으면 전체 목록을 updatedAt 최신순으로 조회하고,
         * status가 있으면 해당 상태만 updatedAt 최신순으로 조회합니다.
         */
        List<ChatRoom> chatRooms = status == null
                ? chatRoomRepository.findAllWithMemberOrderByUpdatedAtDesc()
                : chatRoomRepository.findAllWithMemberByStatusOrderByUpdatedAtDesc(status);

        return chatRooms.stream()
                .map(ChatRoomListResponse::from)
                .toList();
    }

    /**
     * 관리자가 고객 채팅방의 상담 상태를 변경합니다.
     * 권한 규칙:
     * - ADMIN만 변경할 수 있습니다.
     * - MEMBER가 호출하면 FORBIDDEN 예외를 던집니다.
     * 상태 전이 규칙:
     * - WAITING -> IN_PROGRESS 허용
     * - IN_PROGRESS -> COMPLETED 허용
     * - COMPLETED -> 다른 상태 변경 불가
     * - 역방향 전이 불가
     * - 동일 상태 변경 불가
     * 실제 상태 전이 검증은 ChatRoom.changeStatus()에서 처리합니다.
     */
    @Transactional
    public ChatRoom changeChatRoomStatus(MemberRole role, Long chatRoomId, ChatRoomStatus nextStatus) {
        if (role != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.changeStatus(nextStatus);

        return chatRoom;
    }
}
