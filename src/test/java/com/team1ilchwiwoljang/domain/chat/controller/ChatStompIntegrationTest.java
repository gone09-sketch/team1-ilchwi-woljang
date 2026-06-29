package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.dto.request.ChatMessageRequest;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.chat.service.ChatService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatStompIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @MockitoSpyBean
    private ChatService chatService;

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Authorization 헤더 없이 STOMP CONNECT를 보내면 클라이언트가 연결 실패를 관찰한다")
    void given_noAuthorizationHeader_whenConnect_thenClientObservesConnectionFailure() {
        WebSocketStompClient stompClient = createStompClient();
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();

        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(
                webSocketUrl(),
                new WebSocketHttpHeaders(),
                new StompHeaders(),
                sessionHandler
        );

        assertThatThrownBy(() -> connectFuture.get(3, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
        assertThat(sessionHandler.awaitError()).isTrue();
    }

    @Test
    @DisplayName("채팅방 접근 권한이 없는 사용자가 SUBSCRIBE하면 클라이언트가 구독 거부를 관찰한다")
    void given_memberWithoutRoomAccess_whenSubscribe_thenClientObservesSubscribeFailure() throws Exception {
        Member owner = memberRepository.save(createMember("owner@example.com", MemberRole.MEMBER));
        Member otherMember = memberRepository.save(createMember("other@example.com", MemberRole.MEMBER));
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(owner));

        WebSocketStompClient stompClient = createStompClient();
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();
        StompSession stompSession = connect(stompClient, sessionHandler, otherMember);

        stompSession.subscribe(
                "/sub/chat/rooms/" + chatRoom.getId(),
                new NoOpStompFrameHandler()
        );

        assertThat(sessionHandler.awaitError()).isTrue();
    }

    @Test
    @DisplayName("채팅방 소유 회원이 메시지를 보내면 메시지를 저장하고 구독자에게 브로드캐스트한다")
    void given_roomMemberAndSubscription_whenSendMessage_thenSaveAndBroadcastMessage() throws Exception {
        Member member = memberRepository.save(createMember("member@example.com", MemberRole.MEMBER));
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(member));
        String content = "통합 테스트 메시지";

        WebSocketStompClient stompClient = createStompClient();
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();
        StompSession stompSession = connect(stompClient, sessionHandler, member);
        TestStompFrameHandler frameHandler = new TestStompFrameHandler();
        String subscribeDestination = "/sub/chat/rooms/" + chatRoom.getId();

        stompSession.subscribe(
                subscribeDestination,
                frameHandler
        );
        assertThat(awaitSubscription(subscribeDestination)).isTrue();

        stompSession.send(
                "/pub/chat/rooms/" + chatRoom.getId() + "/messages",
                new ChatMessageRequest(content)
        );

        ChatMessageResponse response = frameHandler.awaitResponse();

        assertThat(response.chatRoomId()).isEqualTo(chatRoom.getId());
        assertThat(response.senderId()).isEqualTo(member.getId());
        assertThat(response.content()).isEqualTo(content);
        assertThat(chatMessageRepository.findAll()).hasSize(1)
                .first()
                .satisfies(chatMessage -> {
                    assertThat(chatMessage.getChatRoom().getId()).isEqualTo(chatRoom.getId());
                    assertThat(chatMessage.getSender().getId()).isEqualTo(member.getId());
                    assertThat(chatMessage.getContent()).isEqualTo(content);
                });
    }

    @Test
    @DisplayName("채팅방 접근 권한이 없는 회원이 메시지를 보내면 메시지를 저장하지 않는다")
    void given_memberWithoutRoomAccess_whenSendMessage_thenDoNotSaveMessage() throws Exception {
        Member owner = memberRepository.save(createMember("owner@example.com", MemberRole.MEMBER));
        Member otherMember = memberRepository.save(createMember("other@example.com", MemberRole.MEMBER));
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(owner));

        WebSocketStompClient stompClient = createStompClient();
        TestStompSessionHandler sessionHandler = new TestStompSessionHandler();
        StompSession stompSession = connect(stompClient, sessionHandler, otherMember);

        stompSession.send(
                "/pub/chat/rooms/" + chatRoom.getId() + "/messages",
                new ChatMessageRequest("권한 없는 메시지")
        );

        verify(chatService, timeout(3000)).sendMessage(
                eq(chatRoom.getId()),
                eq(otherMember.getId()),
                any(ChatMessageRequest.class)
        );
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }

    private StompSession connect(
            WebSocketStompClient stompClient,
            TestStompSessionHandler sessionHandler,
            Member member
    ) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(
                AUTHORIZATION_HEADER,
                BEARER_PREFIX + jwtTokenProvider.createAccessToken(member.getId(), member.getRole())
        );

        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(
                webSocketUrl(),
                new WebSocketHttpHeaders(),
                connectHeaders,
                sessionHandler
        );

        return connectFuture.get(3, TimeUnit.SECONDS);
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
        return stompClient;
    }

    private boolean awaitSubscription(String destination) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);

        while (System.nanoTime() < deadline) {
            if (!simpUserRegistry.findSubscriptions(
                    subscription -> destination.equals(subscription.getDestination())
            ).isEmpty()) {
                return true;
            }

            TimeUnit.MILLISECONDS.sleep(50);
        }

        return false;
    }

    private String webSocketUrl() {
        return "ws://localhost:" + port + "/ws";
    }

    private Member createMember(String email, MemberRole role) {
        Member member = Member.create(email, "password", "member", "010-1234-5678");
        ReflectionTestUtils.setField(member, "role", role);
        return member;
    }

    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {

        private final CountDownLatch errorLatch = new CountDownLatch(1);
        private final AtomicReference<Throwable> transportError = new AtomicReference<>();
        private final AtomicReference<StompHeaders> errorFrameHeaders = new AtomicReference<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errorFrameHeaders.set(headers);
            errorLatch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            transportError.set(exception);
            errorLatch.countDown();
        }

        boolean awaitError() {
            try {
                boolean observed = errorLatch.await(3, TimeUnit.SECONDS);
                return observed && (transportError.get() != null || errorFrameHeaders.get() != null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static class NoOpStompFrameHandler implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
        }
    }

    private static class TestStompFrameHandler implements StompFrameHandler {

        private final CountDownLatch messageLatch = new CountDownLatch(1);
        private final AtomicReference<ChatMessageResponse> response = new AtomicReference<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return ChatMessageResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            this.response.set((ChatMessageResponse) payload);
            messageLatch.countDown();
        }

        ChatMessageResponse awaitResponse() throws InterruptedException {
            assertThat(messageLatch.await(3, TimeUnit.SECONDS)).isTrue();
            return response.get();
        }
    }
}
