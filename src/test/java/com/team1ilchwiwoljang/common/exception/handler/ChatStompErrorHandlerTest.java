package com.team1ilchwiwoljang.common.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.util.MimeTypeUtils;

class ChatStompErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatStompErrorHandler errorHandler = new ChatStompErrorHandler();

    @Test
    @DisplayName("STOMP 처리 중 발생한 BusinessException은 공통 ErrorResponse 형식의 ERROR frame으로 변환한다")
    void givenBusinessException_whenHandleError_thenReturnErrorFrameWithErrorResponse() throws Exception {
        BusinessException businessException = new BusinessException(ErrorCode.FORBIDDEN);

        Message<byte[]> result = errorHandler.handleClientMessageProcessingError(null, businessException);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        Map<String, Object> body = objectMapper.readValue(
                result.getPayload(),
                new TypeReference<>() {
                }
        );

        assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(accessor.getMessage()).isEqualTo(ErrorCode.FORBIDDEN.name());
        assertThat(accessor.getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
        assertThat(body)
                .containsEntry("success", false)
                .containsEntry("code", ErrorCode.FORBIDDEN.name())
                .containsEntry("message", ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("Spring Messaging 래퍼 예외 안에 있는 BusinessException도 찾아서 ErrorCode를 유지한다")
    void givenWrappedBusinessException_whenHandleError_thenUseNestedBusinessExceptionCode() {
        RuntimeException wrappedException =
                new RuntimeException(new BusinessException(ErrorCode.COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED));

        Message<byte[]> result = errorHandler.handleClientMessageProcessingError(null, wrappedException);
        String body = new String(result.getPayload(), StandardCharsets.UTF_8);

        assertThat(body).contains(ErrorCode.COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED.name());
        assertThat(body).contains(ErrorCode.COMPLETED_CHAT_ROOM_MESSAGE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("알 수 없는 STOMP 처리 예외는 내부 메시지를 노출하지 않고 채팅 전송 실패로 변환한다")
    void givenUnknownException_whenHandleError_thenReturnGenericChatMessageSendFailed() {
        RuntimeException unknownException = new RuntimeException("internal detail");

        Message<byte[]> result = errorHandler.handleClientMessageProcessingError(null, unknownException);
        String body = new String(result.getPayload(), StandardCharsets.UTF_8);

        assertThat(body).contains(ErrorCode.CHAT_MESSAGE_SEND_FAILED.name());
        assertThat(body).contains(ErrorCode.CHAT_MESSAGE_SEND_FAILED.getMessage());
        assertThat(body).doesNotContain("internal detail");
    }
}
