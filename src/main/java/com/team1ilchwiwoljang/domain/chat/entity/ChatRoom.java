package com.team1ilchwiwoljang.domain.chat.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 1명은 하나의 채팅방만 가질 수 있습니다.
 */
@Getter
@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_member", columnNames = "member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방을 소유한 회원
     * JPA 매핑은 ManyToOne으로 두고,
     * DB unique 제약으로 회원 1명당 채팅방 1개를 보장합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private ChatRoom(Member member) {
        this.member = member;
    }

    public static ChatRoom create(Member member) {
        return new ChatRoom(member);
    }

    /**
     * 회원이 본인의 채팅방에 접근하는지 검증하기 위해 사용합니다.
     * WebSocket 연결 시 회원 권한 검증에 사용됩니다.
     */
    public boolean isOwner(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
