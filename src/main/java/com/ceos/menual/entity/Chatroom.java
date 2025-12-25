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

    // 채팅방 참여자 (회원)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    // 채팅방 참여자 (전문가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @Column(nullable = false)
    private Long consultationId;

    // 채팅방 활성화 여부
    @Column(name = "is_active")
    private boolean isActive;

    @Builder
    public Chatroom(Long consultationId, ChatroomType chatroomType, User member, User expert) {
        this.member = member; // 객체를 받아서 저장
        this.expert = expert;
        this.chatroomType = chatroomType;
        this.consultationId = consultationId;
        this.isActive = true;
    }
}
