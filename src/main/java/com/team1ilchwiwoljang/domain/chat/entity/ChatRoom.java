package com.team1ilchwiwoljang.domain.chat.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    @Column
    private LocalDateTime closedAt;

    public static ChatRoom create(Member member) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.member = member;
        chatRoom.status = ChatRoomStatus.OPEN;
        return chatRoom;
    }

    public boolean isAccessibleBy(Long memberId, MemberRole role) {
        return role == MemberRole.ADMIN || member.getId().equals(memberId);
    }

    public boolean isClosed() {
        return status == ChatRoomStatus.CLOSED;
    }

    public void close(LocalDateTime closedAt) {
        this.status = ChatRoomStatus.CLOSED;
        this.closedAt = closedAt;
    }
}
