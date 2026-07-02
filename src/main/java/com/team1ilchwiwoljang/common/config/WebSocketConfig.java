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
 * STOMP에서는 Spring MessageBroker가 destination 기준으로 구독자에게 메시지를 전달합니다.
 *
 * 현재 destination 설계:
 * 1. 클라이언트 연결 endpoint: /ws/chat
 * 2. 클라이언트가 서버로 메시지 발행: /pub/chat/rooms/{chatRoomId}/messages
 * 3. 클라이언트가 채팅방 메시지 구독: /sub/chat/rooms/{chatRoomId}
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
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /pub 으로 시작하는 destination은 Controller의 @MessageMapping으로 라우팅됩니다.
        registry.setApplicationDestinationPrefixes("/pub");

        // /sub 으로 시작하는 destination은 MessageBroker가 구독자에게 직접 전달합니다.
        registry.enableSimpleBroker("/sub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        /*
         * 클라이언트에서 서버로 들어오는 STOMP 프레임을 가로챕니다.
         *
         * 여기서 처리할 것:
         * 1. CONNECT 시점 JWT 인증
         * 2. SUBSCRIBE 시점 채팅방 접근 권한 검증
         */
        registration.interceptors(chatStompChannelInterceptor);
    }
}
