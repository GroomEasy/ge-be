package com.ceos.menual.entity;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.entity.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chatroom_id")
    private Long chatroomId;

    @Column(name = "user_id")
    private Long senderId;

    // 텍스트 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    // 이미지 url
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @Column(name = "is_read")
    private boolean isRead;

    @Schema(description = "고민지 메세지에는 reservationId, 솔루션지 메세지에는 consultationId")
    @Column(name = "related_id")
    private Long relatedId; // 고민지/솔루션지 ID 등


    @Builder
    public Message(Long chatroomId, Long senderId, String content, String imageUrl, MessageType messageType, Long relatedId) {
        this.chatroomId = chatroomId;
        this.senderId = senderId;
        this.content = content;
        this.imageUrl = imageUrl;
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
                .imageUrl(dto.getImageUrl())
                .messageType(dto.getMessageType())
                .relatedId(dto.getRelatedId())
                .build();
    }

    // Entity -> DTO 변환
    public ChatMessageDTO toDTO() {
        return ChatMessageDTO.builder()
                .messageId(this.id)
                .chatroomId(this.chatroomId)
                .senderId(this.senderId)
                .messageType(this.messageType)
                .content(this.content)
                .imageUrl(this.imageUrl)
                .relatedId(this.relatedId)
                .createdAt(this.getCreatedAt())
                .isRead(this.isRead)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}