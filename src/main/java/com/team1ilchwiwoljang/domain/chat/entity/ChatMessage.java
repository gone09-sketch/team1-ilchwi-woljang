package com.team1ilchwiwoljang.domain.chat.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static ChatMessage create(ChatRoom chatRoom, Member sender, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoom = chatRoom;
        chatMessage.sender = sender;
        chatMessage.senderRole = sender.getRole();
        chatMessage.content = content;
        return chatMessage;
    }
}
