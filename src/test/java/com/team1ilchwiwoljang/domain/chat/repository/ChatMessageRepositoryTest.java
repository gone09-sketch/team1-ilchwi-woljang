package com.team1ilchwiwoljang.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.common.config.JpaConfig;
import com.team1ilchwiwoljang.common.config.QueryDslConfig;
import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("재연결 복구 메시지는 afterMessageId 이후 메시지를 ID 오름차순으로 조회한다")
    void givenAfterMessageId_whenFindMessagesAfterId_thenOrderByIdAsc() {
        Member member = entityManager.persist(Member.create("member@example.com", "password", "member", "010-1234-5678"));
        ChatRoom chatRoom = entityManager.persist(ChatRoom.create(member));
        ChatMessage firstMessage = entityManager.persist(ChatMessage.create(chatRoom, member, "first"));
        ChatMessage secondMessage = entityManager.persist(ChatMessage.create(chatRoom, member, "second"));
        ChatMessage thirdMessage = entityManager.persist(ChatMessage.create(chatRoom, member, "third"));
        entityManager.flush();

        /*
         * Cursor 정책은 messageId 기준입니다.
         * createdAt 순서를 일부러 ID 순서와 다르게 만들어도 조회 결과가 ID 순서를 유지해야 합니다.
         */
        updateCreatedAt(secondMessage, LocalDateTime.of(2026, 7, 1, 12, 0));
        updateCreatedAt(thirdMessage, LocalDateTime.of(2026, 7, 1, 11, 0));
        entityManager.clear();

        List<ChatMessage> messages = chatMessageRepository.findAllWithSenderByChatRoomIdAndIdGreaterThan(
                chatRoom.getId(),
                firstMessage.getId()
        );

        assertThat(messages)
                .extracting(ChatMessage::getId)
                .containsExactly(secondMessage.getId(), thirdMessage.getId());
    }

    private void updateCreatedAt(ChatMessage message, LocalDateTime createdAt) {
        EntityManager em = entityManager.getEntityManager();

        em.createQuery("""
                UPDATE ChatMessage message
                SET message.createdAt = :createdAt
                WHERE message.id = :messageId
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("messageId", message.getId())
                .executeUpdate();
    }
}
