package com.team1ilchwiwoljang.domain.chat.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * ChatHandshakeInterceptor에서 이미 검증된 chatRoomId를 꺼내서
 * 채팅방별로 WebSocketSession을 저장합니다.
 * 권한 검증은 여기서 다시 하지 않습니다.
 * Handler는 "이미 연결이 허용된 세션"만 관리합니다.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * 채팅방 ID별 WebSocket 세션 목록입니다.
     * key: chatRoomId
     * value: 해당 채팅방에 연결된 WebSocketSession 목록
     * 여러 사용자가 동시에 연결/해제될 수 있으므로 ConcurrentHashMap을 사용합니다.
     */
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long chatRoomId = getChatRoomId(session);

        roomSessions
                .computeIfAbsent(chatRoomId, id -> ConcurrentHashMap.newKeySet())
                .add(session);

        session.sendMessage(new TextMessage("채팅방에 연결되었습니다. chatRoomId=" + chatRoomId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long chatRoomId = getChatRoomId(session);
        Long senderId = (Long) session.getAttributes().get("memberId");
        MemberRole role = (MemberRole) session.getAttributes().get("role");

        String payload = message.getPayload();

        // 현재 단계에서는 메시지 저장 없이 같은 채팅방에 연결된 세션들에게만 전달합니다.
        // 이후 ChatMessage Entity를 만들면 여기에서 DB 저장 후 broadcast하면 됩니다.
        String broadcastMessage = "[" + role + ":" + senderId + "] " + payload;

        for (WebSocketSession targetSession : roomSessions.getOrDefault(chatRoomId, Set.of())) {
            if (targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(broadcastMessage));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long chatRoomId = getChatRoomId(session);

        Set<WebSocketSession> sessions = roomSessions.get(chatRoomId);

        if (sessions == null) {
            return;
        }

        sessions.remove(session);

        // 채팅방에 남은 연결이 없으면 Map에서 제거합니다.
        if (sessions.isEmpty()) {
            roomSessions.remove(chatRoomId);
        }
    }

    private Long getChatRoomId(WebSocketSession session) {
        return (Long) session.getAttributes().get("chatRoomId");
    }
}

