package com.ceos.menual.domain.chat.dto.request;

import com.ceos.menual.entity.enums.ChatroomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatroomCreateRequestDTO {

    // consultationId는 url path variable에 포함됨
    private Long consultationId;

    private ChatroomType chatroomType;
}
