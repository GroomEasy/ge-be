package com.ceos.menual.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocketResponseDTO<T> {

    private String eventType;
    private Long chatroomId;
    private T payload;         // 실제 데이터

    public static <T> SocketResponseDTO<T> message(Long chatroomId, T payload) {
        return new SocketResponseDTO<>("MESSAGE", chatroomId, payload);
    }
}