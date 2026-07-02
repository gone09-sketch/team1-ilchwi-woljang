package com.team1ilchwiwoljang.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomListResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("동시 생성으로 DB unique 제약이 발생하면 채팅방 중복 예외로 변환한다")
    void createMyChatRoomThrowsDuplicateWhenUniqueConstraintFails() {
        Long memberId = 1L;
        Member member = createMember(memberId, MemberRole.MEMBER);

        given(memberService.findById(memberId)).willReturn(Optional.of(member));
        given(chatRoomRepository.existsByMember_Id(memberId)).willReturn(false);
        given(chatRoomRepository.saveAndFlush(any(ChatRoom.class)))
                .willThrow(new DataIntegrityViolationException("duplicate chat room"));

        assertThatThrownBy(() -> chatRoomService.createMyChatRoom(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("관리자 채팅방 목록은 전체 목록으로 조회한다")
    void getChatRoomsReturnsAllChatRoomsWhenStatusIsNull() {
        Member member = createMember(1L, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(10L, member);

        given(chatRoomRepository.findAllWithMemberOrderByUpdatedAtDesc())
                .willReturn(List.of(chatRoom));

        List<ChatRoomListResponse> response =
                chatRoomService.getChatRooms(MemberRole.ADMIN, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).chatRoomId()).isEqualTo(10L);
        assertThat(response.get(0).memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("관리자 채팅방 목록은 상태별로 조회할 수 있다")
    void getChatRoomsReturnsFilteredChatRoomsWhenStatusExists() {
        Member member = createMember(1L, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(10L, member);

        given(chatRoomRepository.findAllWithMemberByStatusOrderByUpdatedAtDesc(ChatRoomStatus.WAITING))
                .willReturn(List.of(chatRoom));

        List<ChatRoomListResponse> response =
                chatRoomService.getChatRooms(MemberRole.ADMIN, ChatRoomStatus.WAITING);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(ChatRoomStatus.WAITING);
    }

    @Test
    @DisplayName("회원은 전체 채팅방 목록을 조회할 수 없다")
    void getChatRoomsThrowsForbiddenForMember() {
        assertThatThrownBy(() -> chatRoomService.getChatRooms(MemberRole.MEMBER, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    private ChatRoom createChatRoom(Long id, Member member) {
        ChatRoom chatRoom = ChatRoom.create(member);
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private Member createMember(Long id, MemberRole role) {
        Member member = Member.create("member" + id + "@example.com", "password", "member", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }
}
