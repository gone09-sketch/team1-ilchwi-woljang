package com.team1ilchwiwoljang.domain.chat.controller;

import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.repository.ChatMessageRepository;
import com.team1ilchwiwoljang.domain.chat.repository.ChatRoomRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        stompClient.setMessageConverter(new StringMessageConverter());
        return stompClient;
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
}
