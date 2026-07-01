package com.team1ilchwiwoljang.domain.chat.interceptor;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.security.JwtTokenPayload;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 연결 요청이 ChatWebSocketHandler까지 도달하기 전에
 * 인증과 채팅방 접근 권한을 먼저 검증하는 Interceptor입니다.
 * 여기서 검증할 내용:
 * 1. 요청에 accessToken이 있는지 확인
 * 2. 요청에 chatRoomId가 있는지 확인
 * 3. JWT에서 memberId, role 추출
 * 4. 실제 존재하는 회원인지 확인
 * 5. 해당 회원 또는 관리자가 chatRoomId에 접근 가능한지 확인
 * 6. 검증된 정보를 WebSocketSession attributes에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final ChatRoomService chatRoomService;

    /**
     * WebSocket 연결 전에 실행
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        /*
         * 클라이언트가 요청한 URI를 가져옵니다
         * 예: /ws/chat?chatRoomId=1&accessToken=xxx
         */
        URI uri = request.getURI();

        log.info("WebSocket handshake 요청: path={}", uri.getPath());

        // URI 에서 query parameter를 꺼냅니다.
        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String chatRoomIdValue = queryParams.getFirst("chatRoomId");
        String accessToken = queryParams.getFirst("accessToken");

        // 채팅방 ID나 토큰이 없으면 WebSocket 연결 자체를 거부합니다.
        if (chatRoomIdValue == null || accessToken == null || accessToken.isBlank()) {
            log.warn(
                    "WebSocket handshake 실패: 필수 파라미터 누락, hasChatRoomId={}, hasAccessToken={}",
                    chatRoomIdValue != null,
                    accessToken != null && !accessToken.isBlank()
            );

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long chatRoomId;

        try {
            chatRoomId = Long.valueOf(chatRoomIdValue);
        } catch (NumberFormatException e) {
            log.warn("WebSocket handshake 실패: chatRoomId 형식 오류, chatRoomId={}", chatRoomIdValue);

            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // JWT에서 회원 ID와 권한을 꺼냅니다. (JWT Access Token 파싱)
        JwtTokenPayload tokenPayload;

        try {
            tokenPayload = jwtTokenProvider.parseAccessToken(accessToken);
        } catch (RuntimeException e) {
            log.warn("WebSocket handshake 실패: accessToken 파싱 실패, chatRoomId={}", chatRoomId);

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long memberId = tokenPayload.memberId();
        MemberRole role = tokenPayload.role();

        // 탈퇴했거나 존재하지 않는 회원이면 연결을 거부합니다.
        if (!memberService.existsActiveMember(memberId)) {
            log.warn(
                    "WebSocket handshake 실패: 존재하지 않거나 탈퇴한 회원, memberId={}, role={}, chatRoomId={}",
                    memberId,
                    role,
                    chatRoomId
            );

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        /*
         * 권한 검증입니다.
         * MEMBER는 본인에게 할당된 채팅방만 통과하고,
         * ADMIN은 모든 회원 채팅방을 통과합니다.
         */
        ChatRoom chatRoom;

        try {
            chatRoom = chatRoomService.getAccessibleChatRoom(memberId, role, chatRoomId);
        } catch (BusinessException e) {
            log.warn(
                    "WebSocket handshake 실패: 채팅방 접근 권한 없음, memberId={}, role={}, chatRoomId={}",
                    memberId,
                    role,
                    chatRoomId
            );

            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        /*
         * Handler에서 사용할 수 있도록
         * 검증된 회원ID를 WebSocketSession attributes에 저장합니다.
         */
        attributes.put("memberId", memberId);
        attributes.put("role", role);
        attributes.put("chatRoomId", chatRoom.getId());

        log.info(
                "WebSocket handshake 성공: memberId={}, role={}, chatRoomId={}",
                memberId,
                role,
                chatRoom.getId()
        );

        return true;
    }

    /**
     * WebSocket 연결 후에 실행
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 현재는 후처리가 필요 없습니다.
    }
}