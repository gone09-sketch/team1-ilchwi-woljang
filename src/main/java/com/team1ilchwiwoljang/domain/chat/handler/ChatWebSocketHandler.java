package com.team1ilchwiwoljang.domain.chat.handler;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.ErrorResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.team1ilchwiwoljang.domain.chat.dto.response.ChatMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * ChatHandshakeInterceptor에서 이미 검증된 chatRoomId를 꺼내서
 * 채팅방별로 WebSocketSession을 저장합니다.
 * 권한 검증은 여기서 다시 하지 않습니다.
 * Handler는 "이미 연결이 허용된 세션"만 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MILLISECONDS = 10_000;
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 ID별 WebSocket 세션 목록입니다.
     * key: chatRoomId
     * value: sessionId를 key로 하는 해당 채팅방의 WebSocketSession 목록
     * 같은 방에서 연결/해제/브로드캐스트가 동시에 발생하므로 최상위 Map과 방 내부 Map을 모두 동시성 컬렉션으로 둡니다.
     */
    private final Map<Long, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long chatRoomId = getChatRoomId(session);
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLISECONDS,
                SEND_BUFFER_SIZE_LIMIT_BYTES
        );

        /*
         * compute를 사용하면 같은 chatRoomId에 대한 추가/삭제가 하나의 원자적 연산으로 처리됩니다.
         * 연결 종료와 새 연결이 겹칠 때 새 세션이 들어간 Map을 닫는 쪽에서 통째로 지워버리는 레이스를 막기 위한 처리입니다.
         */
        roomSessions.compute(chatRoomId, (id, sessions) -> {
            Map<String, WebSocketSession> currentSessions =
                    sessions == null ? new ConcurrentHashMap<>() : sessions;
            currentSessions.put(session.getId(), safeSession);
            return currentSessions;
        });

        sendMessageSafely(
                chatRoomId,
                safeSession,
                new TextMessage("채팅방에 연결되었습니다. chatRoomId=" + chatRoomId)
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long chatRoomId = getChatRoomId(session);
        Long senderId = getMemberId(session);
        MemberRole role = getRole(session);

        String payload = message.getPayload();
        ChatMessageResponse savedMessage;

        try {
            // WebSocket으로 받은 메시지는 브로드캐스트 전에 먼저 DB에 저장하여, 전송된 메시지와 저장된 이력이 어긋나지 않게 합니다.
            savedMessage = chatMessageService.saveMessage(
                    senderId,
                    role,
                    chatRoomId,
                    payload
            );
        } catch (BusinessException e) {
            sendErrorMessage(session, e.getErrorCode());
            return;
        } catch (RuntimeException e) {
            log.error("채팅 메시지 저장 실패: chatRoomId={}, senderId={}", chatRoomId, senderId, e);
            sendErrorMessage(session, ErrorCode.CHAT_MESSAGE_SEND_FAILED);
            return;
        }

        String broadcastMessage;

        try {
            broadcastMessage = objectMapper.writeValueAsString(savedMessage);
        } catch (RuntimeException e) {
            log.error("채팅 메시지 직렬화 실패: chatRoomId={}, messageId={}", chatRoomId, savedMessage.messageId(), e);
            sendErrorMessage(session, ErrorCode.CHAT_MESSAGE_SEND_FAILED);
            return;
        }

        /*
         * 각 세션 전송 실패를 개별 처리합니다.
         * 한 세션의 네트워크 문제나 동시 쓰기 문제가 같은 방의 다른 세션 브로드캐스트까지 중단시키면 안 됩니다.
         */
        Map<String, WebSocketSession> sessions = roomSessions.getOrDefault(chatRoomId, Map.of());

        for (WebSocketSession targetSession : sessions.values()) {
            sendMessageSafely(chatRoomId, targetSession, new TextMessage(broadcastMessage));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long chatRoomId = getChatRoomId(session);

        if (chatRoomId == null) {
            return;
        }

        /*
         * 연결 종료와 새 연결이 같은 방에서 동시에 일어날 수 있으므로 computeIfPresent로 삭제를 원자화합니다.
         * 닫힌 세션만 제거하고, 방이 비었을 때만 최상위 Map에서 제거합니다.
         */
        roomSessions.computeIfPresent(chatRoomId, (id, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    private void sendMessageSafely(Long chatRoomId, WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            removeSession(chatRoomId, session.getId());
            return;
        }

        try {
            session.sendMessage(message);
        } catch (Exception e) {
            log.warn(
                    "채팅 메시지 전송 실패: chatRoomId={}, sessionId={}",
                    chatRoomId,
                    session.getId(),
                    e
            );
            removeSession(chatRoomId, session.getId());
        }
    }

    private void sendErrorMessage(WebSocketSession session, ErrorCode errorCode) {
        Long chatRoomId = getChatRoomId(session);

        try {
            String errorPayload = objectMapper.writeValueAsString(
                    ErrorResponse.of(errorCode.name(), errorCode.getMessage())
            );
            sendMessageSafely(chatRoomId, session, new TextMessage(errorPayload));
        } catch (RuntimeException e) {
            log.warn("WebSocket 에러 응답 직렬화 실패: chatRoomId={}, sessionId={}", chatRoomId, session.getId(), e);
            sendMessageSafely(chatRoomId, session, new TextMessage(errorCode.getMessage()));
        }
    }

    private void removeSession(Long chatRoomId, String sessionId) {
        if (chatRoomId == null || sessionId == null) {
            return;
        }

        roomSessions.computeIfPresent(chatRoomId, (id, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
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
