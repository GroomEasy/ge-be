package com.ceos.menual.domain.chat.dto.response;

import com.ceos.menual.entity.Message;
import com.ceos.menual.entity.enums.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponseDTO {
    private Long messageId;
    private Long senderId;
    private String senderRole;  // EXPERT 또는 MEMBER
    private String senderNickname;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private Long relatedId;
    private LocalDateTime createdAt;

    public static ChatMessageResponseDTO from(
            Message message,
            String senderNickname,
            String senderRole) {
        return ChatMessageResponseDTO.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderRole(senderRole)
                .senderNickname(senderNickname)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .relatedId(message.getRelatedId())
                .createdAt(message.getCreatedAt())
                .build();
    }
}