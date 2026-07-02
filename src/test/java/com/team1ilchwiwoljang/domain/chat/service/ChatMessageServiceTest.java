package com.team1ilchwiwoljang.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("afterMessageId가 없으면 채팅방 메시지를 전체 조회한다")
    void givenNoAfterMessageId_whenGetMessages_thenFindAllMessages() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        ChatRoom chatRoom = createChatRoom(chatRoomId);

        given(chatRoomService.getAccessibleChatRoom(memberId, MemberRole.MEMBER, chatRoomId))
                .willReturn(chatRoom);
        given(chatMessageRepository.findAllWithSenderByChatRoomId(chatRoomId))
                .willReturn(List.of());

        List<ChatMessageResponse> response =
                chatMessageService.getMessages(memberId, MemberRole.MEMBER, chatRoomId, null);

        assertThat(response).isEmpty();
        verify(chatMessageRepository).findAllWithSenderByChatRoomId(chatRoomId);
        verify(chatMessageRepository, never())
                .findAllWithSenderByChatRoomIdAndIdGreaterThan(any(), any());
    }

    @Test
    @DisplayName("afterMessageId가 있으면 해당 메시지 ID 이후 메시지만 조회한다")
    void givenAfterMessageId_whenGetMessages_thenFindMessagesAfterId() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        Long afterMessageId = 100L;
        ChatRoom chatRoom = createChatRoom(chatRoomId);

        given(chatRoomService.getAccessibleChatRoom(memberId, MemberRole.MEMBER, chatRoomId))
                .willReturn(chatRoom);
        given(chatMessageRepository.findAllWithSenderByChatRoomIdAndIdGreaterThan(chatRoomId, afterMessageId))
                .willReturn(List.of());

        List<ChatMessageResponse> response =
                chatMessageService.getMessages(memberId, MemberRole.MEMBER, chatRoomId, afterMessageId);

        assertThat(response).isEmpty();
        verify(chatMessageRepository)
                .findAllWithSenderByChatRoomIdAndIdGreaterThan(chatRoomId, afterMessageId);
        verify(chatMessageRepository, never()).findAllWithSenderByChatRoomId(chatRoomId);
    }

    @Test
    @DisplayName("이미 완료된 채팅방에는 메시지를 저장할 수 없다")
    void givenCompletedChatRoom_whenSaveMessage_thenThrowException() {
        Long senderId = 1L;
        Long chatRoomId = 1L;
        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        ChatRoom completedChatRoom = ChatRoom.create(member);
        completedChatRoom.changeStatus(ChatRoomStatus.IN_PROGRESS);
        completedChatRoom.changeStatus(ChatRoomStatus.COMPLETED);

        given(chatRoomService.getAccessibleChatRoom(senderId, MemberRole.MEMBER, chatRoomId))
                .willReturn(completedChatRoom);

        assertThatThrownBy(() -> chatMessageService.saveMessage(
                senderId,
                MemberRole.MEMBER,
                chatRoomId,
                "완료된 채팅방에 보내는 메시지"
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode",
                        ErrorCode.COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED
                );

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("빈 메시지는 저장하지 않고 검증 예외를 던진다")
    void saveMessageThrowsValidationWhenContentIsBlank() {
        assertThatThrownBy(() -> chatMessageService.saveMessage(1L, MemberRole.MEMBER, 10L, " "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("1000자를 초과한 메시지는 저장하지 않고 검증 예외를 던진다")
    void saveMessageThrowsValidationWhenContentExceedsLimit() {
        String content = "a".repeat(1001);

        assertThatThrownBy(() -> chatMessageService.saveMessage(1L, MemberRole.MEMBER, 10L, content))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    private ChatRoom createChatRoom(Long chatRoomId) {
        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        ChatRoom chatRoom = ChatRoom.create(member);
        ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
        return chatRoom;
    }
}
