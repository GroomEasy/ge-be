package com.ceos.menual.entity;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.entity.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chatroom_id")
    private Long chatroomId;

    @Column(name = "user_id")
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type")
    private MessageType messageType; // Enum으로 관리한다면 MessageType으로 변경 권장

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "related_id")
    private Long relatedId; // 고민지/솔루션지 ID 등

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Message(Long chatroomId, Long senderId, String content, MessageType messageType, Long relatedId) {
        this.chatroomId = chatroomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.relatedId = relatedId;
        this.isRead = false; // 기본값 false
    }

    // DTO -> Entity 변환
    public static Message create(ChatMessageDTO dto) {
        return Message.builder()
                .chatroomId(dto.getChatroomId())
                .senderId(dto.getSenderId())
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .relatedId(dto.getRelatedId())
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}