package com.team1ilchwiwoljang.domain.chat.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class ChatWebSocketHandlerTest {

    @Test
    @DisplayName("get 후 null이면 put하는 방식은 동시 접속 시 세션이 누락될 수 있다")
    void givenGetThenPut_whenSessionsConnectConcurrently_thenSomeSessionsCanBeLost() throws Exception {
        Map<Long, Set<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

        int sessionCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(sessionCount);
        CountDownLatch readyToPut = new CountDownLatch(sessionCount);
        CountDownLatch startPut = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < sessionCount; i++) {
                int index = i;

                futures.add(executorService.submit(() -> {
                    Long chatRoomId = 1L;
                    WebSocketSession session = createSession(index);

                    Set<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);

                    if (sessions == null) {
                        sessions = ConcurrentHashMap.newKeySet();

                        readyToPut.countDown();
                        assertThat(startPut.await(3, TimeUnit.SECONDS)).isTrue();

                        chatRoomSessions.put(chatRoomId, sessions);
                    }

                    sessions.add(session);
                    return null;
                }));
            }

            assertThat(readyToPut.await(3, TimeUnit.SECONDS)).isTrue();
            startPut.countDown();

            for (Future<?> future : futures) {
                future.get(3, TimeUnit.SECONDS);
            }

            assertThat(chatRoomSessions.get(1L)).hasSizeLessThan(sessionCount);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("computeIfAbsent 방식은 동시 접속 시에도 모든 세션을 누락 없이 등록한다")
    void givenComputeIfAbsent_whenSessionsConnectConcurrently_thenRegisterAllSessions() throws Exception {
        Map<Long, Set<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

        int sessionCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(sessionCount);
        CountDownLatch ready = new CountDownLatch(sessionCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < sessionCount; i++) {
                int index = i;

                futures.add(executorService.submit(() -> {
                    Long chatRoomId = 1L;
                    WebSocketSession session = createSession(index);

                    ready.countDown();
                    assertThat(start.await(3, TimeUnit.SECONDS)).isTrue();

                    chatRoomSessions
                            .computeIfAbsent(chatRoomId, key -> ConcurrentHashMap.newKeySet())
                            .add(session);
                    return null;
                }));
            }

            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<?> future : futures) {
                future.get(3, TimeUnit.SECONDS);
            }

            assertThat(chatRoomSessions.get(1L)).hasSize(sessionCount);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("WebSocket 연결이 성공하면 chatRoomId 기준으로 세션을 등록한다")
    void givenChatRoomId_whenConnectionEstablished_thenRegisterSession() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession session = createSession(1, 1L);

        handler.afterConnectionEstablished(session);

        Map<Long, Set<WebSocketSession>> chatRoomSessions = getChatRoomSessions(handler);
        assertThat(chatRoomSessions.get(1L)).containsExactly(session);
    }

    @Test
    @DisplayName("WebSocket 연결이 종료되면 채팅방 세션 목록에서 제거한다")
    void givenRegisteredSession_whenConnectionClosed_thenRemoveSession() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession session = createSession(1, 1L);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        Map<Long, Set<WebSocketSession>> chatRoomSessions = getChatRoomSessions(handler);
        assertThat(chatRoomSessions).doesNotContainKey(1L);
    }

    @Test
    @DisplayName("메시지는 같은 chatRoomId에 연결된 세션에만 전송된다")
    void givenDifferentChatRoomSessions_whenMessageReceived_thenSendOnlySameChatRoom() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler();
        WebSocketSession sender = createSession(1, 1L);
        WebSocketSession sameRoomSession = createSession(2, 1L);
        WebSocketSession otherRoomSession = createSession(3, 2L);
        when(sender.isOpen()).thenReturn(true);
        when(sameRoomSession.isOpen()).thenReturn(true);
        when(otherRoomSession.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(sameRoomSession);
        handler.afterConnectionEstablished(otherRoomSession);
        clearInvocations(sender, sameRoomSession, otherRoomSession);

        handler.handleTextMessage(sender, new TextMessage("hello"));

        verify(sender).sendMessage(new TextMessage("hello"));
        verify(sameRoomSession).sendMessage(new TextMessage("hello"));
        verify(otherRoomSession, never()).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession createSession(int index) {
        return createSession(index, 1L);
    }

    private WebSocketSession createSession(int index, Long chatRoomId) {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getUri())
                .thenReturn(URI.create("ws://localhost:8080/ws/chat?chatRoomId=" + chatRoomId));
        when(session.getId())
                .thenReturn("session-" + index);

        return session;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Set<WebSocketSession>> getChatRoomSessions(ChatWebSocketHandler handler) {
        return (Map<Long, Set<WebSocketSession>>) ReflectionTestUtils.getField(
                handler,
                "chatRoomSessions"
        );
    }
}
