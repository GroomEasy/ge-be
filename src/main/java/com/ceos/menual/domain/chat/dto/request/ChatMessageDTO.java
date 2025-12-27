package com.ceos.menual.domain.chat.dto.request;

import com.ceos.menual.entity.enums.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDTO {

    private Long chatroomId;
    private Long senderId;
    private MessageType messageType;
    private String content;
    private String imageUrl;
    private Long relatedId; // 연관 리소스, 일반 텍스트면 null
}