package com.team1ilchwiwoljang.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("채팅방 메시지는 페이지 응답으로 반환한다")
    void getMessagesReturnsPageResponse() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        Member sender = createMember(memberId, MemberRole.MEMBER);
        ChatRoom chatRoom = createChatRoom(chatRoomId, sender);
        ChatMessage message = ChatMessage.create(chatRoom, sender, "안녕하세요");
        ReflectionTestUtils.setField(message, "id", 100L);

        given(chatRoomService.getAccessibleChatRoom(memberId, MemberRole.MEMBER, chatRoomId))
                .willReturn(chatRoom);
        given(chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(eq(chatRoomId), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(message)));

        PageResponse<ChatMessageResponse> response =
                chatMessageService.getMessages(memberId, MemberRole.MEMBER, chatRoomId, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).messageId()).isEqualTo(100L);
        assertThat(response.content().get(0).chatRoomId()).isEqualTo(chatRoomId);
        assertThat(response.content().get(0).content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("빈 메시지는 저장하지 않고 검증 예외를 던진다")
    void saveMessageThrowsValidationWhenContentIsBlank() {
        assertThatThrownBy(() -> chatMessageService.saveMessage(1L, MemberRole.MEMBER, 10L, " "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("1000자를 초과한 메시지는 저장하지 않고 검증 예외를 던진다")
    void saveMessageThrowsValidationWhenContentExceedsLimit() {
        String content = "a".repeat(1001);

        assertThatThrownBy(() -> chatMessageService.saveMessage(1L, MemberRole.MEMBER, 10L, content))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
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
