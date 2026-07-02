package com.team1ilchwiwoljang.domain.chatbot.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotConversationStateTest {

    private MutableClock clock;
    private ChatbotConversationState conversationState;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-07-01T00:00:00Z"));
        conversationState = new ChatbotConversationState(clock);
    }

    @Test
    @DisplayName("같은 대화를 계속 사용하면 마지막 사용 시간이 갱신되어 만료되지 않는다")
    void given_sameConversationWithinTtl_whenTouch_thenExtendExpiration() {
        conversationState.touch("conversation-1");

        clock.advance(Duration.ofMinutes(29));
        ChatbotConversationState.ConversationTouchResult firstResult =
                conversationState.touch("conversation-1");

        clock.advance(Duration.ofMinutes(29));
        ChatbotConversationState.ConversationTouchResult secondResult =
                conversationState.touch("conversation-1");

        assertThat(firstResult.expiredConversationIds()).isEmpty();
        assertThat(firstResult.currentConversationExpired()).isFalse();
        assertThat(secondResult.expiredConversationIds()).isEmpty();
        assertThat(secondResult.currentConversationExpired()).isFalse();
    }

    @Test
    @DisplayName("마지막 사용 후 30분이 지난 대화는 만료 대상으로 반환한다")
    void given_expiredConversation_whenTouch_thenReturnExpiredConversationId() {
        conversationState.touch("conversation-1");

        clock.advance(Duration.ofMinutes(31));
        ChatbotConversationState.ConversationTouchResult result =
                conversationState.touch("conversation-1");

        assertThat(result.expiredConversationIds()).containsExactly("conversation-1");
        assertThat(result.currentConversationExpired()).isTrue();
    }

    @Test
    @DisplayName("다른 대화가 요청되어도 마지막 사용 후 30분이 지난 대화만 만료 대상으로 반환한다")
    void given_multipleConversations_whenTouch_thenReturnOnlyExpiredConversations() {
        conversationState.touch("expired-conversation");

        clock.advance(Duration.ofMinutes(31));
        ChatbotConversationState.ConversationTouchResult result =
                conversationState.touch("active-conversation");

        assertThat(result.expiredConversationIds()).containsExactly("expired-conversation");
        assertThat(result.currentConversationExpired()).isFalse();
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
