package com.team1ilchwiwoljang.common.config;

import com.team1ilchwiwoljang.domain.chat.interceptor.ChatStompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 설정입니다.
 * 현재 destination 규칙:
 * - WebSocket/STOMP 연결 endpoint: /ws/chat
 * - 클라이언트 메시지 발행: /pub/chat/rooms/{chatRoomId}/messages
 * - 클라이언트 메시지 구독: /sub/chat/rooms/{chatRoomId}
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompChannelInterceptor chatStompChannelInterceptor;

    @Value("${chat.websocket.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        /*
         * STOMP 연결 endpoint입니다.
         * 클라이언트는 이 endpoint로 WebSocket 연결을 맺은 뒤,
         * STOMP CONNECT frame을 보냅니다.
         */
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /*
         * 클라이언트가 /pub 으로 시작하는 destination에 SEND하면
         * Spring이 @MessageMapping Controller로 라우팅합니다.
         */
        registry.setApplicationDestinationPrefixes("/pub");

        /*
         * 클라이언트가 /sub 으로 시작하는 destination을 SUBSCRIBE하면
         * SimpleBroker가 해당 destination 구독자에게 메시지를 전달합니다.
         */
        registry.enableSimpleBroker("/sub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트에서 서버로 들어오는 STOMP frame을 가로챕니다.
        registration.interceptors(chatStompChannelInterceptor);
    }
}
