package com.team1ilchwiwoljang.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("이미 완료된 채팅방에는 메시지를 저장할 수 없다")
    void givenCompletedChatRoom_whenSaveMessage_thenThrowException() {
        Long senderId = 1L;
        Long chatRoomId = 1L;
        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        ChatRoom completedChatRoom = ChatRoom.create(member);
        completedChatRoom.changeStatus(ChatRoomStatus.IN_PROGRESS);
        completedChatRoom.changeStatus(ChatRoomStatus.COMPLETED);

        /*
         * saveMessage()는 메시지를 저장하기 전에 먼저 채팅방 접근 권한을 확인합니다.
         * 이 테스트에서는 권한 검증은 통과했고, 반환된 채팅방이 이미 COMPLETED인 상황만 검증합니다.
         */
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

        /*
         * 완료된 채팅방이면 sender 조회나 메시지 저장까지 진행되면 안 됩니다.
         * 여기서는 저장 Repository가 호출되지 않았는지만 명확히 확인합니다.
         */
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}
