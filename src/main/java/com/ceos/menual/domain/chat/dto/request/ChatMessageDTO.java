package com.ceos.menual.domain.chat.dto.request;

import com.ceos.menual.entity.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private Long messageId;
    private Long chatroomId;
    private Long senderId;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private Long relatedId; // 연관 리소스, 일반 텍스트면 null
    private LocalDateTime createdAt;
    private Boolean isRead;
}