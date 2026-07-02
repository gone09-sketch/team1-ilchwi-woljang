package com.team1ilchwiwoljang.common.config;

import com.team1ilchwiwoljang.domain.chat.handler.ChatWebSocketHandler;
import com.team1ilchwiwoljang.domain.chat.interceptor.ChatHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Value("${chat.websocket.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                // WebSocket 연결 전에 JWT 인증과 채팅방 접근 권한을 검증합니다.
                .addInterceptors(chatHandshakeInterceptor)
                /*
                 * 모든 Origin을 항상 허용하면 외부 사이트에서도 브라우저 WebSocket 연결을 시도할 수 있습니다.
                 * 환경 변수 CHAT_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS로 운영 도메인만 열 수 있게 제한합니다.
                 */
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
