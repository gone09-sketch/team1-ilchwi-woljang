package com.team1ilchwiwoljang.domain.chat.listener;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.chat.auth.ChatPrincipal;
import com.team1ilchwiwoljang.domain.chat.dto.response.ChatSystemMessageResponse;
import com.team1ilchwiwoljang.domain.chat.service.ChatMessageService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP 채팅방 구독 생명주기에 맞춰 시스템 메시지를 발행하는 Listener입니다.
 *
 * 역할 분리:
 * - ChatStompChannelInterceptor: CONNECT 인증, SUBSCRIBE 권한 검증
 * - 이 Listener: 권한 검증을 통과한 구독/구독 해제/연결 종료 이벤트에 맞춰 시스템 메시지 발행
 *
 * 시스템 메시지는 DB에 저장하지 않고 STOMP destination으로만 실시간 전달합니다.
 */
@Component
@RequiredArgsConstructor
public class ChatStompSubscribeEventListener {

    private static final String CHAT_ROOM_SUB_PREFIX = "/sub/chat/rooms/";

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /*
     * 현재 서버 인스턴스에서 유지 중인 채팅방 구독 정보입니다.
     *
     * 왜 필요한가:
     * - SUBSCRIBE event에는 destination이 들어있지만,
     * - UNSUBSCRIBE/DISCONNECT event에서는 어떤 채팅방 구독이 해제됐는지 바로 알기 어렵습니다.
     *
     * 그래서 SUBSCRIBE 시점에 sessionId + subscriptionId 기준으로 구독 정보를 저장해두고,
     * 이후 UNSUBSCRIBE 또는 DISCONNECT 시점에 꺼내서 퇴장 시스템 메시지를 발행합니다.
     */
    private final Map<String, ChatSubscription> subscriptions = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        // 채팅방 구독 destination이 아니면 시스템 메시지 대상이 아닙니다.
        if (destination == null || !destination.startsWith(CHAT_ROOM_SUB_PREFIX)) {
            return;
        }

        Long chatRoomId = extractChatRoomId(destination);
        ChatPrincipal principal = getChatPrincipal(accessor);

        /*
         * 이후 UNSUBSCRIBE/DISCONNECT에서 어떤 채팅방을 나갔는지 알 수 있도록
         * 현재 구독 정보를 먼저 저장합니다.
         */
        rememberSubscription(accessor, chatRoomId, principal.role(), destination);

        /*
         * 입장 시스템 메시지 정책:
         * - 저장된 실제 채팅 메시지가 아직 없으면 입장 시스템 메시지를 보냅니다.
         * - 이미 실제 채팅 메시지가 있으면 재입장으로 보고 입장 시스템 메시지를 보내지 않습니다.
         * - 시스템 메시지는 DB에 저장하지 않으므로 이 판단에 포함되지 않습니다.
         */
        if (chatMessageService.hasMessages(chatRoomId)) {
            return;
        }

        publishSystemMessage(
                destination,
                ChatSystemMessageResponse.entered(chatRoomId, principal.role())
        );
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        /*
         * 명시적으로 채팅방 구독을 해제한 경우입니다.
         * 저장해둔 구독 정보가 없으면 채팅방 구독이 아니거나 이미 DISCONNECT에서 처리된 요청이므로 무시합니다.
         */
        ChatSubscription subscription = removeSubscription(accessor);

        if (subscription == null) {
            return;
        }

        publishSystemMessage(
                subscription.destination(),
                ChatSystemMessageResponse.exited(subscription.chatRoomId(), subscription.role())
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            return;
        }

        /*
         * 브라우저 종료, 네트워크 단절, STOMP 연결 종료처럼
         * 명시적인 UNSUBSCRIBE 없이 세션이 끊기는 경우를 처리합니다.
         *
         * 한 세션이 여러 구독을 가지고 있을 수 있으므로 sessionId에 묶인 구독을 모두 찾습니다.
         * 현재 정책은 "현재 채팅방 1개만 구독"이지만, 서버 코드는 중복 구독이 들어와도 정리 가능하게 둡니다.
         */
        List<ChatSubscription> disconnectedSubscriptions = subscriptions.values()
                .stream()
                .filter(subscription -> subscription.sessionId().equals(sessionId))
                .toList();

        for (ChatSubscription subscription : disconnectedSubscriptions) {
            /*
             * UNSUBSCRIBE와 DISCONNECT가 연달아 들어오는 경우 중복 퇴장 메시지가 나갈 수 있습니다.
             * remove(key, value)가 성공한 구독만 실제 퇴장 메시지를 발행해서 중복을 막습니다.
             */
            boolean removed = subscriptions.remove(subscriptionKey(
                    subscription.sessionId(),
                    subscription.subscriptionId()
            ), subscription);

            if (removed) {
                publishSystemMessage(
                        subscription.destination(),
                        ChatSystemMessageResponse.exited(subscription.chatRoomId(), subscription.role())
                );
            }
        }
    }

    private void rememberSubscription(
            StompHeaderAccessor accessor,
            Long chatRoomId,
            MemberRole role,
            String destination
    ) {
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        /*
         * STOMP SUBSCRIBE에는 일반적으로 subscription id가 포함됩니다.
         * 둘 중 하나가 없다면 이후 정확한 구독 해제 추적이 어렵기 때문에 저장하지 않습니다.
         */
        if (sessionId == null || subscriptionId == null) {
            return;
        }

        subscriptions.put(
                subscriptionKey(sessionId, subscriptionId),
                new ChatSubscription(sessionId, subscriptionId, chatRoomId, role, destination)
        );
    }

    private ChatSubscription removeSubscription(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (sessionId == null || subscriptionId == null) {
            return null;
        }

        return subscriptions.remove(subscriptionKey(sessionId, subscriptionId));
    }

    private String subscriptionKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
    }

    private void publishSystemMessage(String destination, ChatSystemMessageResponse systemMessage) {
        /*
         * 서버는 채팅방별 destination으로만 메시지를 발행합니다.
         * 실제 전달 대상 계산은 Spring Simple Broker가 해당 destination 구독자 기준으로 처리합니다.
         */
        messagingTemplate.convertAndSend(destination, systemMessage);
    }

    private Long extractChatRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(CHAT_ROOM_SUB_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private ChatPrincipal getChatPrincipal(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        /*
         * ChatStompChannelInterceptor가 CONNECT 단계에서 설정한 Principal만 신뢰합니다.
         * Principal이 없거나 타입이 다르면 정상적인 STOMP 인증 과정을 거치지 않은 요청입니다.
         */
        if (!(principal instanceof ChatPrincipal chatPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return chatPrincipal;
    }

    private record ChatSubscription(
            String sessionId,
            String subscriptionId,
            Long chatRoomId,
            MemberRole role,
            String destination
    ) {
    }
}
