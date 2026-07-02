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

    /**
     * 특정 채팅방에 저장된 실제 채팅 메시지가 하나라도 있는지 확인합니다.
     * 시스템 입장 메시지를 보낼지 판단할 때 사용합니다.
     * 정책:
     * - 채팅방에 저장된 메시지가 아직 0개이면 입장 시스템 메시지를 보냅니다.
     * - 저장된 메시지가 1개 이상이면 이미 상담이 시작된 것으로 보고
     *   재입장 시에는 시스템 메시지를 다시 보내지 않습니다.
     */
    boolean existsByChatRoom_Id(Long chatRoomId);
}