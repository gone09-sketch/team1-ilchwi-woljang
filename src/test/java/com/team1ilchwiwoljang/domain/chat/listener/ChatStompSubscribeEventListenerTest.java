package com.team1ilchwiwoljang.domain.chat.listener;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.domain.chat.auth.ChatPrincipal;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatSystemMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class ChatStompSubscribeEventListenerTest {

    private static final String SESSION_ID = "session-1";
    private static final String SUBSCRIPTION_ID = "sub-1";

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatStompSubscribeEventListener listener;

    @Test
    @DisplayName("메시지가 없는 채팅방을 구독하면 입장 시스템 메시지를 발행한다")
    void givenEmptyChatRoom_whenSubscribe_thenPublishEnteredSystemMessage() {
        Long chatRoomId = 10L;
        String destination = "/sub/chat/rooms/" + chatRoomId;
        given(chatMessageService.hasMessages(chatRoomId)).willReturn(false);

        listener.handleSubscribe(new SessionSubscribeEvent(
                this,
                createStompMessage(StompCommand.SUBSCRIBE, destination)
        ));

        verify(messagingTemplate).convertAndSend(
                eq(destination),
                argThat((ChatSystemMessageResponse message) -> isSystemMessageForChatRoom(message, chatRoomId))
        );
    }

    @Test
    @DisplayName("구독 해제하면 저장된 구독 정보 기준으로 퇴장 시스템 메시지를 발행한다")
    void givenSubscribedChatRoom_whenUnsubscribe_thenPublishExitedSystemMessage() {
        Long chatRoomId = 10L;
        String destination = "/sub/chat/rooms/" + chatRoomId;
        given(chatMessageService.hasMessages(chatRoomId)).willReturn(true);

        listener.handleSubscribe(new SessionSubscribeEvent(
                this,
                createStompMessage(StompCommand.SUBSCRIBE, destination)
        ));
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(
                this,
                createStompMessage(StompCommand.UNSUBSCRIBE, destination)
        ));

        verify(messagingTemplate).convertAndSend(
                eq(destination),
                argThat((ChatSystemMessageResponse message) -> isSystemMessageForChatRoom(message, chatRoomId))
        );
    }

    @Test
    @DisplayName("구독 해제 후 연결 종료가 다시 들어와도 퇴장 시스템 메시지는 중복 발행하지 않는다")
    void givenAlreadyUnsubscribed_whenDisconnect_thenDoNotPublishExitedSystemMessageAgain() {
        Long chatRoomId = 10L;
        String destination = "/sub/chat/rooms/" + chatRoomId;
        given(chatMessageService.hasMessages(chatRoomId)).willReturn(true);

        listener.handleSubscribe(new SessionSubscribeEvent(
                this,
                createStompMessage(StompCommand.SUBSCRIBE, destination)
        ));
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(
                this,
                createStompMessage(StompCommand.UNSUBSCRIBE, destination)
        ));
        listener.handleDisconnect(new SessionDisconnectEvent(
                this,
                createStompMessage(StompCommand.DISCONNECT, destination),
                SESSION_ID,
                CloseStatus.NORMAL
        ));

        verify(messagingTemplate, times(1)).convertAndSend(
                eq(destination),
                argThat((ChatSystemMessageResponse message) -> isSystemMessageForChatRoom(message, chatRoomId))
        );
    }

    private Message<byte[]> createStompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(SESSION_ID);
        accessor.setSubscriptionId(SUBSCRIPTION_ID);
        accessor.setDestination(destination);
        accessor.setUser(new ChatPrincipal(1L, MemberRole.MEMBER));

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private boolean isSystemMessageForChatRoom(ChatSystemMessageResponse message, Long chatRoomId) {
        return message != null
                && "SYSTEM".equals(message.type())
                && chatRoomId.equals(message.chatRoomId());
    }
}
