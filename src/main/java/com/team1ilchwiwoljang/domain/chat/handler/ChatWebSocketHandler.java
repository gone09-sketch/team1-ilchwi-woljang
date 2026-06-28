package com.team1ilchwiwoljang.domain.chat.handler;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long chatRoomId = getChatRoomId(session);

        chatRoomSessions
                .computeIfAbsent(chatRoomId, key -> ConcurrentHashMap.newKeySet())
                .add(session);

        log.info("WebSocket 연결 완료: chatRoomId={}, sessionId={}", chatRoomId, session.getId());

        session.sendMessage(new TextMessage("채팅방에 입장했습니다."));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long chatRoomId = getChatRoomId(session);
        String payload = message.getPayload();

        log.info("메시지 수신: chatRoomId={}, sessionId={}, messageLength={}",
                chatRoomId,
                session.getId(),
                payload.length());

        Set<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);

        if (sessions == null) {
            return;
        }

        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) {
                sessions.remove(s);
                continue;
            }

            try {
                s.sendMessage(new TextMessage(payload));
            } catch (Exception e) {
                sessions.remove(s);
                log.warn("WebSocket 메시지 전송 실패로 세션 제거: chatRoomId={}, sessionId={}",
                        chatRoomId,
                        s.getId(),
                        e);
            }
        }

        if (sessions.isEmpty()) {
            chatRoomSessions.remove(chatRoomId, sessions);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long chatRoomId = getChatRoomId(session);

        Set<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);

        if (sessions != null) {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                chatRoomSessions.remove(chatRoomId, sessions);
            }
        }

        log.info("WebSocket 연결 종료: chatRoomId={}, sessionId={}, status={}",
                chatRoomId,
                session.getId(),
                status);
    }

    private Long getChatRoomId(WebSocketSession session) {
        URI uri = session.getUri();

        if (uri == null) {
            throw new IllegalStateException("WebSocket URI가 존재하지 않습니다.");
        }

        String chatRoomId = UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("chatRoomId");

        if (chatRoomId == null) {
            throw new IllegalArgumentException("chatRoomId가 존재하지 않습니다.");
        }

        try {
            return Long.valueOf(chatRoomId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("chatRoomId는 숫자여야 합니다.");
        }
    }
}