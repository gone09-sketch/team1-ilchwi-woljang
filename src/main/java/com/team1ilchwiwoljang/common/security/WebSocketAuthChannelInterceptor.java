package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.chat.service.ChatService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHAT_ROOM_SUBSCRIBE_PREFIX = "/sub/chat/rooms/";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor));
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor);
        }

        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Authentication is required");
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        if (accessToken.isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        try {
            JwtTokenPayload tokenPayload = jwtTokenProvider.parseAccessToken(accessToken);
            Long memberId = tokenPayload.memberId();
            MemberRole role = tokenPayload.role();

            if (!memberService.existsActiveMember(memberId)) {
                throw new AccessDeniedException("Authentication is required");
            }

            AuthMember authMember = new AuthMember(memberId, role);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());

            return new UsernamePasswordAuthenticationToken(
                    authMember,
                    null,
                    List.of(authority)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("Authentication is required", e);
        }
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        Long chatRoomId = extractChatRoomId(accessor.getDestination());

        if (chatRoomId == null) {
            return;
        }

        AuthMember authMember = getAuthMember(accessor.getUser());
        chatService.validateRoomAccess(chatRoomId, authMember.memberId(), authMember.role());
    }

    private Long extractChatRoomId(String destination) {
        if (destination == null || !destination.startsWith(CHAT_ROOM_SUBSCRIBE_PREFIX)) {
            return null;
        }

        String chatRoomId = destination.substring(CHAT_ROOM_SUBSCRIBE_PREFIX.length());

        if (chatRoomId.isBlank() || chatRoomId.contains("/")) {
            throw new AccessDeniedException("Invalid chat room destination");
        }

        try {
            return Long.valueOf(chatRoomId);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid chat room destination", e);
        }
    }

    private AuthMember getAuthMember(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthMember authMember) {
            return authMember;
        }

        throw new AccessDeniedException("Authentication is required");
    }
}
