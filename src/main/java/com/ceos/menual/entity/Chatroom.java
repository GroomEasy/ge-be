package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.ChatroomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chatrooms")
public class Chatroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatroomType chatroomType;

    // 채팅방 참여자(MEMBER)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 채팅방 참여자(EXPERT)
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(nullable = false)
    private Long consultationId;

    // 채팅방 활성화 여부: 상담 종료 시 false
    @Column(name = "is_active")
    private boolean isActive;

    @Builder
    public Chatroom(Long consultationId, ChatroomType chatroomType, Long memberId, Long expertId) {
        this.memberId = memberId;
        this.chatroomType = chatroomType;
        this.expertId = expertId;
        this.isActive = true; // 생성 시 기본 활성화
        this.consultationId = consultationId; // 여기!
    }
}
