package com.ceos.menual.entity;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.entity.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "messages")
public class Message extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chatroom_id")
    private Long chatroomId;

    @Column(name = "user_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read")
    private boolean isRead = false; // 기본값 안 읽음

    public static Message create(ChatMessageDTO dto) {
        Message msg = new Message();
        msg.setChatroomId(dto.getChatroomId());
        msg.setSenderId(dto.getSenderId());
        msg.setMessageType(dto.getMessageType());
        msg.setContent(dto.getContent());
        return msg;
    }
}