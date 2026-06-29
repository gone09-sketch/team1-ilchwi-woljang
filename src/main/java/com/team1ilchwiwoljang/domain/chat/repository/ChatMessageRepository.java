package com.team1ilchwiwoljang.domain.chat.repository;

import com.team1ilchwiwoljang.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
}
