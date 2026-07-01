package com.team1ilchwiwoljang.domain.chat.repository;

import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지를 대화 흐름 순서대로 조회합니다.
     * 채팅 화면에서 처음 보낸 메시지부터 최신 메시지까지
     * 위에서 아래로 보여주기 위해 createdAt 오름차순으로 정렬합니다.
     */
    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
}