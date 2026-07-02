package com.team1ilchwiwoljang.domain.chat.repository;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

import com.team1ilchwiwoljang.domain.chat.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 회원 ID로 해당 회원에게 할당된 채팅방을 조회합니다.
    Optional<ChatRoom> findByMember_Id(Long memberId);

    // 회원에게 이미 채팅방이 할당되어 있는지 확인합니다.
    boolean existsByMember_Id(Long memberId);

    /**
     * 관리자가 전체 고객 채팅방 목록을 조회할 때 사용합니다.
     * 정렬 기준:
     * - updatedAt DESC: 최근 상태가 변경된 채팅방이 먼저 보이도록 정렬합니다.
     * - createdAt DESC: updatedAt이 같은 경우 최신 생성 채팅방이 먼저 오도록 보조 정렬합니다.
     */
    @Query("""
        SELECT chatRoom
        FROM ChatRoom chatRoom
        JOIN FETCH chatRoom.member
        ORDER BY chatRoom.updatedAt DESC, chatRoom.createdAt DESC
        """)
    List<ChatRoom> findAllWithMemberOrderByUpdatedAtDesc();

    // 관리자가 상담 상태별로 고객 채팅방 목록을 조회할 때 사용합니다.
    @Query("""
        SELECT chatRoom
        FROM ChatRoom chatRoom
        JOIN FETCH chatRoom.member
        WHERE chatRoom.status = :status
        ORDER BY chatRoom.updatedAt DESC, chatRoom.createdAt DESC
        """)
    List<ChatRoom> findAllWithMemberByStatusOrderByUpdatedAtDesc(
            @Param("status") ChatRoomStatus status
    );
}
