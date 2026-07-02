package com.team1ilchwiwoljang.domain.chat.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.security.JwtTokenPayload;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.chat.auth.ChatPrincipal;
import com.team1ilchwiwoljang.domain.chat.service.ChatRoomService;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class ChatStompChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberService memberService;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatStompChannelInterceptor chatStompChannelInterceptor;

    @Test
    @DisplayName("CONNECT 프레임의 JWT 인증이 성공하면 ChatPrincipal을 설정한다")
    void givenValidToken_whenConnect_thenSetChatPrincipal() {
        String accessToken = "access-token";
        Long memberId = 1L;

        given(jwtTokenProvider.parseAccessToken(accessToken))
                .willReturn(new JwtTokenPayload(memberId, MemberRole.MEMBER));
        given(memberService.existsActiveMember(memberId)).willReturn(true);

        Message<byte[]> message = createConnectMessage(accessToken);

        Message<?> result = chatStompChannelInterceptor.preSend(message, mock(MessageChannel.class));

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isInstanceOf(ChatPrincipal.class);
        assertThat(((ChatPrincipal) resultAccessor.getUser()).memberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("채팅방 SUBSCRIBE 프레임은 Principal 기준으로 채팅방 접근 권한을 검증한다")
    void givenChatRoomSubscribe_whenPreSend_thenValidateChatRoomAccess() {
        Long memberId = 1L;
        Long chatRoomId = 10L;
        Message<byte[]> message = createSubscribeMessage(
                "/sub/chat/rooms/" + chatRoomId,
                new ChatPrincipal(memberId, MemberRole.MEMBER)
        );

        chatStompChannelInterceptor.preSend(message, mock(MessageChannel.class));

        verify(chatRoomService).getAccessibleChatRoom(memberId, MemberRole.MEMBER, chatRoomId);
    }

    private Message<byte[]> createConnectMessage(String accessToken) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + accessToken);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> createSubscribeMessage(String destination, ChatPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
