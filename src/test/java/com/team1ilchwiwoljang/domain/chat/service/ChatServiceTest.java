package com.team1ilchwiwoljang.domain.chat.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatRoomResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("회원 ID로 채팅방을 생성하면 OPEN 상태 채팅방 응답을 반환한다")
    void given_memberId_whenCreateChatRoom_thenReturnOpenRoom() {
        Long memberId = 1L;
        Member member = createMember(memberId, MemberRole.MEMBER);

        given(memberService.findById(memberId)).willReturn(Optional.of(member));
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom chatRoom = invocation.getArgument(0);
            ReflectionTestUtils.setField(chatRoom, "id", 10L);
            return chatRoom;
        });

        ChatRoomResponse response = chatService.createChatRoom(memberId);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.status()).isEqualTo(ChatRoomStatus.OPEN);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 참여 회원이 메시지를 보내면 메시지를 저장하고 응답을 반환한다")
    void given_roomMemberAndMessage_whenSendMessage_thenSaveMessage() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        Member member = createMember(memberId, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(chatRoomId, member);
        ChatMessageRequest request = new ChatMessageRequest("안녕하세요");

        given(memberService.findById(memberId)).willReturn(Optional.of(member));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage chatMessage = invocation.getArgument(0);
            ReflectionTestUtils.setField(chatMessage, "id", 100L);
            return chatMessage;
        });

        ChatMessageResponse response = chatService.sendMessage(chatRoomId, memberId, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
        assertThat(response.senderId()).isEqualTo(memberId);
        assertThat(response.senderRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(response.content()).isEqualTo("안녕하세요");
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("관리자는 다른 회원의 채팅방에도 메시지를 보낼 수 있다")
    void given_admin_whenSendMessageToMemberRoom_thenSaveMessage() {
        Long ownerId = 1L;
        Long adminId = 2L;
        Long chatRoomId = 10L;
        Member owner = createMember(ownerId, MemberRole.MEMBER);
        Member admin = createMember(adminId, MemberRole.ADMIN);
        ChatRoom chatRoom = createChatRoom(chatRoomId, owner);
        ChatMessageRequest request = new ChatMessageRequest("관리자 답변입니다");

        given(memberService.findById(adminId)).willReturn(Optional.of(admin));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponse response = chatService.sendMessage(chatRoomId, adminId, request);

        assertThat(response.senderId()).isEqualTo(adminId);
        assertThat(response.senderRole()).isEqualTo(MemberRole.ADMIN);
        assertThat(response.content()).isEqualTo("관리자 답변입니다");
    }

    @Test
    @DisplayName("채팅방 주인이 아닌 일반 회원이 메시지를 보내면 CHAT_ROOM_ACCESS_DENIED 예외가 발생한다")
    void given_otherMember_whenSendMessage_thenThrowAccessDenied() {
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long chatRoomId = 10L;
        Member owner = createMember(ownerId, MemberRole.MEMBER);
        Member otherMember = createMember(otherMemberId, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(chatRoomId, owner);
        ChatMessageRequest request = new ChatMessageRequest("권한 없는 메시지");

        given(memberService.findById(otherMemberId)).willReturn(Optional.of(otherMember));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.sendMessage(chatRoomId, otherMemberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @Test
    @DisplayName("채팅방 메시지 목록 조회 시 접근 권한을 확인하고 생성순으로 반환한다")
    void given_accessibleRoom_whenGetMessages_thenReturnMessages() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        Member member = createMember(memberId, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(chatRoomId, member);
        ChatMessage first = createMessage(100L, chatRoom, member, "첫 번째 메시지");
        ChatMessage second = createMessage(101L, chatRoom, member, "두 번째 메시지");

        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(chatRoomId))
                .willReturn(List.of(first, second));

        List<ChatMessageResponse> responses = chatService.getMessages(chatRoomId, memberId, MemberRole.MEMBER);

        assertThat(responses)
                .extracting(ChatMessageResponse::content)
                .containsExactly("첫 번째 메시지", "두 번째 메시지");
    }

    @Test
    @DisplayName("채팅방 주인이 아닌 일반 회원이 메시지 목록을 조회하면 CHAT_ROOM_ACCESS_DENIED 예외가 발생한다")
    void given_otherMember_whenGetMessages_thenThrowAccessDenied() {
        Long ownerId = 1L;
        Long otherMemberId = 2L;
        Long chatRoomId = 10L;
        Member owner = createMember(ownerId, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(chatRoomId, owner);

        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> chatService.getMessages(chatRoomId, otherMemberId, MemberRole.MEMBER))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        verify(chatMessageRepository, never()).findAllByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
    }

    private Member createMember(Long memberId, MemberRole role) {
        Member member = Member.create("member" + memberId + "@example.com", "password", "member", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }

    private ChatRoom createChatRoom(Long chatRoomId, Member member) {
        ChatRoom chatRoom = ChatRoom.create(member);
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }

    private ChatMessage createMessage(Long messageId, ChatRoom chatRoom, Member sender, String content) {
        ChatMessage chatMessage = ChatMessage.create(chatRoom, sender, content);
        ReflectionTestUtils.setField(chatMessage, "id", messageId);
        return chatMessage;
    }
}
