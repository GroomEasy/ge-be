package com.ceos.menual.domain.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageReadResponseDTO {
    private Integer readCount;  // 읽음 처리된 메시지 개수

    public static MessageReadResponseDTO of(int count) {
        return MessageReadResponseDTO.builder()
                .readCount(count)
                .build();
    }
}