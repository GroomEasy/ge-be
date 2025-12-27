package com.ceos.menual.domain.chat.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class MessageReadRequestDTO {
    private List<Long> messageIds;  // 읽음 처리할 메시지 ID 목록
}