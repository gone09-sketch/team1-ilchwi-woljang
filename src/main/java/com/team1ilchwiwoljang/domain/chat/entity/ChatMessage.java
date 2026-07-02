package com.team1ilchwiwoljang.domain.chat.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 안에서 주고받은 메시지를 저장하는 Entity입니다.
 */
@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 메시지가 속한 채팅방입니다.
     * 회원은 본인의 채팅방 메시지만 조회할 수 있고,
     * 관리자는 모든 채팅방 메시지를 조회할 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 메시지를 보낸 회원입니다.
     * 회원이 보낸 메시지일 수도 있고,
     * 관리자가 답변으로 보낸 메시지일 수도 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    /**
     * 메시지 발신자의 역할입니다.
     * sender.getRole()로도 확인할 수 있지만,
     * 메시지 생성 당시의 역할을 명확하게 남기기 위해 함께 저장합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole senderRole;


    // 실제 채팅 메시지 내용입니다.
    @Column(nullable = false, length = 1000)
    private String content;

    private ChatMessage(ChatRoom chatRoom, Member sender, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.senderRole = sender.getRole();
        this.content = content;
    }

    public static ChatMessage create(ChatRoom chatRoom, Member sender, String content) {
        return new ChatMessage(chatRoom, sender, content);
    }
}
