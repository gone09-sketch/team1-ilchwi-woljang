package com.team1ilchwiwoljang.domain.chatbot.state;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotConversationState {

    private static final Duration CONVERSATION_TTL = Duration.ofMinutes(30);

    private final Clock clock;

    /**
     * conversationId별 마지막 요청 시간을 저장합니다.
     * 삭제 기준은 conversationId 생성 시간이 아니라 마지막 사용 시간이므로,
     * 사용자가 계속 대화 중이면 같은 conversationId의 만료 시간이 계속 연장됩니다.
     */
    private final Map<String, Instant> lastAccessedAtByConversation = new ConcurrentHashMap<>();

    public ChatbotConversationState() {
        this(Clock.systemDefaultZone());
    }

    ChatbotConversationState(Clock clock) {
        this.clock = clock;
    }

    /**
     * 챗봇 요청이 들어올 때마다 호출합니다.
     * 오래전에 만들어진 conversationId라도 최근에 사용됐다면 삭제하지 않고,
     * 마지막 사용 후 30분이 지난 conversationId만 정리 대상으로 반환합니다.
     */
    public ConversationTouchResult touch(String conversationId) {
        Instant now = Instant.now(clock);
        List<String> expiredConversationIds = findExpiredConversationIds(now);
        boolean currentConversationExpired = expiredConversationIds.contains(conversationId);

        expiredConversationIds.forEach(lastAccessedAtByConversation::remove);
        lastAccessedAtByConversation.put(conversationId, now);

        return new ConversationTouchResult(
                expiredConversationIds,
                currentConversationExpired
        );
    }

    private List<String> findExpiredConversationIds(Instant now) {
        return lastAccessedAtByConversation.entrySet()
                .stream()
                .filter(entry -> isExpired(entry.getValue(), now))
                .map(Map.Entry::getKey)
                .toList();
    }

    private boolean isExpired(Instant lastAccessedAt, Instant now) {
        return lastAccessedAt
                .plus(CONVERSATION_TTL)
                .isBefore(now);
    }

    public record ConversationTouchResult(
            List<String> expiredConversationIds,
            boolean currentConversationExpired
    ) {
    }
}
