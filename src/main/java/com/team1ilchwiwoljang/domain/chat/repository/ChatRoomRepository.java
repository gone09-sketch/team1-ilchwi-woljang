package com.team1ilchwiwoljang.domain.chat.repository;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 회원 ID로 해당 회원에게 할당된 채팅방을 조회합니다.
    Optional<ChatRoom> findByMember_Id(Long memberId);

    // 회원에게 이미 채팅방이 할당되어 있는지 확인합니다.
    boolean existsByMember_Id(Long memberId);
}
