package com.team1ilchwiwoljang.domain.inquiry.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column
    private Long adminId; // 관리자 ID (답변 완료 시 설정, nullable)

    @Column(length = 1000)
    private String answer; // 관리자 답변 내용 (nullable)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status;

    public static Inquiry create(Member member, String title, String content) {
        Inquiry inquiry = new Inquiry();
        inquiry.member = member;
        inquiry.title = title;
        inquiry.content = content;
        inquiry.status = InquiryStatus.WAITING;
        return inquiry;
    }
}
