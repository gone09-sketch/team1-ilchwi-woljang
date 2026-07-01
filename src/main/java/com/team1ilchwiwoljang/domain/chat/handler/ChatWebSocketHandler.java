package com.team1ilchwiwoljang.domain.chat.handler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * ChatHandshakeInterceptor에서 이미 검증된 chatRoomId를 꺼내서
 * 채팅방별로 WebSocketSession을 저장합니다.
 * 권한 검증은 여기서 다시 하지 않습니다.
 * Handler는 "이미 연결이 허용된 세션"만 관리합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

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

        /*
         * chatRoomId에 해당하는 세션 목록이 없으면 새로 만들고,
         * 이미 있으면 기존 목록에 현재 세션을 추가합니다.
         */
        roomSessions
                .computeIfAbsent(chatRoomId, id -> ConcurrentHashMap.newKeySet())
                .add(session);

        session.sendMessage(new TextMessage("채팅방에 연결되었습니다. chatRoomId=" + chatRoomId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long chatRoomId = getChatRoomId(session);
        Long senderId = getMemberId(session);
        MemberRole role = getRole(session);

        String payload = message.getPayload();

        // 1. WebSocket으로 받은 메시지를 먼저 DB에 저장합니다.
        ChatMessageResponse savedMessage = chatMessageService.saveMessage(
                senderId,
                role,
                chatRoomId,
                payload
        );

        // 2. 저장된 메시지를 JSON 문자열로 변환합니다.
        String broadcastMessage = objectMapper.writeValueAsString(savedMessage);

        // 3. 같은 채팅방에 연결된 세션들에게만 메시지를 전달합니다.
        Set<WebSocketSession> sessions = roomSessions.getOrDefault(chatRoomId, Set.of());

        for (WebSocketSession targetSession : sessions) {
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

        // 현재 종료된 세션을 채팅방 세션 목록에서 제거합니다.
        sessions.remove(session);

        /*
         * 채팅방에 더 이상 접속자가 없으면 Map에서도 제거하여
         * 비어있는 Set이 계속 남아있지 않게 합니다.
         */
        if (sessions.isEmpty()) {
            roomSessions.remove(chatRoomId);
        }
    }

    /**
     * ChatHandshakeInterceptor에서 저장한 chatRoomId를 꺼냅니다.
     * 이 값은 WebSocket 연결 전에 이미 검증된 채팅방 ID입니다.
     */
    private Long getChatRoomId(WebSocketSession session) {
        return (Long) session.getAttributes().get("chatRoomId");
    }


    /**
     * ChatHandshakeInterceptor에서 저장한 memberId를 꺼냅니다.
     * 메시지를 누가 보냈는지 구분하기 위해 사용합니다.
     */
    private Long getMemberId(WebSocketSession session) {
        return (Long) session.getAttributes().get("memberId");
    }

    /**
     * ChatHandshakeInterceptor에서 저장한 role을 꺼냅니다.
     * MEMBER가 보낸 메시지인지, ADMIN이 보낸 메시지인지 구분하기 위해 사용합니다.
     */
    private MemberRole getRole(WebSocketSession session) {
        return (MemberRole) session.getAttributes().get("role");
    }
}

