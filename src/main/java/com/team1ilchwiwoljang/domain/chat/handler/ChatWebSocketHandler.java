package com.team1ilchwiwoljang.domain.chat.handler;

import java.util.HashSet;
import java.util.Set;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 실제 WebSocket 통신 로직 담당 클래스
 * 연결이 되었을 때, 메시지를 받았을 때, 연결이 끊어졌을 때 무엇을 할지 정의합니다.
 */
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 현재 연결된 모든 WebSocket 세션을 저장합니다.
    private final Set<WebSocketSession> sessions = new HashSet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.sendMessage(new TextMessage("WebSocket 연결이 완료되었습니다."));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        for (WebSocketSession s : sessions) {
            s.sendMessage(new TextMessage("서버에서 받은 메시지: " + message.getPayload()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }
}

