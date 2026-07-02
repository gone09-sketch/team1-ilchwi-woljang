package com.team1ilchwiwoljang.domain.chat.repository;

import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지를 대화 흐름 순서대로 조회합니다.
     * 메시지 응답을 만들 때 sender 정보가 필요하므로
     * sender를 fetch join으로 함께 조회합니다.
     * chatRoom도 응답의 chatRoomId를 만들 때 사용하므로 함께 fetch join합니다.
     */
    @Query("""
        SELECT message
        FROM ChatMessage message
        JOIN FETCH message.sender
        JOIN FETCH message.chatRoom
        WHERE message.chatRoom.id = :chatRoomId
        ORDER BY message.createdAt ASC
        """)
    List<ChatMessage> findAllWithSenderByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
