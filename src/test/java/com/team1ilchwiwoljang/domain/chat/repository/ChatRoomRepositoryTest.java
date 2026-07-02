package com.team1ilchwiwoljang.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.common.config.JpaConfig;
import com.team1ilchwiwoljang.common.config.QueryDslConfig;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
@ActiveProfiles("test")
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("상태별 채팅방 목록은 최근 변경순으로 조회한다")
    void givenChatRoomsWithSameStatus_whenFindByStatus_thenOrderByUpdatedAtDesc() {
        ChatRoom oldUpdatedRoom = persistChatRoom("old@example.com");
        ChatRoom recentlyUpdatedRoom = persistChatRoom("recent@example.com");

        /*
         * Repository 쿼리의 정렬 기준만 검증하기 위해 timestamp를 명시적으로 고정합니다.
         * Auditing 동작 자체는 이 테스트의 목적이 아니므로, 테스트 데이터만 직접 구성합니다.
         */
        setBaseTime(oldUpdatedRoom, LocalDateTime.of(2026, 7, 1, 10, 0));
        setBaseTime(recentlyUpdatedRoom, LocalDateTime.of(2026, 7, 1, 11, 0));
        entityManager.flush();
        entityManager.clear();

        List<ChatRoom> chatRooms =
                chatRoomRepository.findAllWithMemberByStatusOrderByUpdatedAtDesc(ChatRoomStatus.WAITING);

        assertThat(chatRooms)
                .extracting(ChatRoom::getId)
                .containsExactly(recentlyUpdatedRoom.getId(), oldUpdatedRoom.getId());
    }

    private ChatRoom persistChatRoom(String email) {
        Member member = entityManager.persist(Member.create(email, "password", "member", "010-1234-5678"));
        ChatRoom chatRoom = entityManager.persist(ChatRoom.create(member));
        entityManager.flush();

        return chatRoom;
    }

    private void setBaseTime(ChatRoom chatRoom, LocalDateTime updatedAt) {
        EntityManager em = entityManager.getEntityManager();

        em.createQuery("""
                UPDATE ChatRoom chatRoom
                SET chatRoom.updatedAt = :updatedAt,
                    chatRoom.createdAt = :createdAt
                WHERE chatRoom.id = :chatRoomId
                """)
                .setParameter("updatedAt", updatedAt)
                .setParameter("createdAt", updatedAt.minusHours(1))
                .setParameter("chatRoomId", chatRoom.getId())
                .executeUpdate();

        ReflectionTestUtils.setField(chatRoom, "updatedAt", updatedAt);
        ReflectionTestUtils.setField(chatRoom, "createdAt", updatedAt.minusHours(1));
    }
}
