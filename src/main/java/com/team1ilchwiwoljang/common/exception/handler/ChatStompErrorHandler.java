package com.team1ilchwiwoljang.common.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.ErrorResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
public class ChatStompErrorHandler extends StompSubProtocolErrorHandler {

    /*
     * STOMP ERROR frame body에는 ErrorResponse만 직렬화합니다.
     * 애플리케이션 전역 ObjectMapper Bean 유무와 무관하게 동작해야 하므로
     * handler 내부에서 전용 ObjectMapper를 사용합니다.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 클라이언트가 보낸 STOMP frame을 처리하다 발생한 예외를 ERROR frame으로 변환합니다.
     *
     * HTTP 요청은 GlobalExceptionHandler가 ErrorResponse 형식으로 변환하지만,
     * STOMP CONNECT/SUBSCRIBE/SEND 처리 중 발생한 예외는 HTTP Controller 흐름을 지나지 않습니다.
     * 따라서 STOMP 계층에서는 별도의 ErrorHandler가 같은 응답 형식을 만들어야 합니다.
     */
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        ErrorCode errorCode = resolveErrorCode(ex);

        try {
            ErrorResponse response = ErrorResponse.of(errorCode.name(), errorCode.getMessage());
            byte[] payload = objectMapper.writeValueAsBytes(response);

            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
            /*
             * STOMP ERROR frame의 message header에는 짧은 식별자를 넣고,
             * 자세한 응답 형식은 body JSON에 담습니다.
             * 프론트는 header만 보거나 body.code/body.message를 파싱해 분기할 수 있습니다.
             */
            accessor.setMessage(errorCode.name());
            accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
            accessor.setContentLength(payload.length);
            accessor.setLeaveMutable(true);

            return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
        } catch (JsonProcessingException serializationException) {
            /*
             * ErrorResponse 직렬화 자체가 실패한 경우에는 Spring 기본 ERROR frame 생성 로직에 위임합니다.
             * 이 상황은 응답 변환 계층의 예외이므로 원래 예외를 감추지 않고 기본 처리로 넘깁니다.
             */
            return super.handleClientMessageProcessingError(clientMessage, ex);
        }
    }

    private ErrorCode resolveErrorCode(Throwable ex) {
        /*
         * Spring Messaging은 ChannelInterceptor 또는 @MessageMapping에서 발생한 예외를
         * MessageDeliveryException 같은 래퍼 예외로 감쌀 수 있습니다.
         * 그래서 최상위 예외만 확인하지 않고 cause chain에서 BusinessException을 찾습니다.
         */
        Throwable current = ex;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException.getErrorCode();
            }

            current = current.getCause();
        }

        /*
         * 예상하지 못한 STOMP 처리 실패는 내부 예외 메시지를 클라이언트에 노출하지 않습니다.
         * 채팅 도메인에서는 일반적인 메시지 전송 실패 코드로 내려 프론트가 안전하게 처리하게 합니다.
         */
        return ErrorCode.CHAT_MESSAGE_SEND_FAILED;
    }
}
