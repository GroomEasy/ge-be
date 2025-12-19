package com.ceos.menual.domain.chat.controller;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.dto.response.SocketResponseDTO;
import com.ceos.menual.domain.chat.exception.ChatErrorCode;
import com.ceos.menual.domain.chat.service.ChatService;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    // 클라이언트가 /pub/chat/message 로 보낸 메시지를 처리
    @MessageMapping("/chat/message")
    public void message(ChatMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {

        // 세션에서 userId(senderId) 추출
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Object userIdObj = sessionAttributes.get("userId");

        if (userIdObj == null) {
            log.error("세션에 userId가 없습니다. (인증 실패)");
            throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
        }

        Long senderId = Long.parseLong(String.valueOf(userIdObj));

        // DTO에 보낸 사람 ID 주입
        message.setSenderId(senderId);

        // 메시지 타입에 따른 시스템 로직
        if ("SYSTEM".equals(message.getMessageType())) {
            message.setContent("시스템 알림: " + message.getContent());
        }

        try {
            // DB 저장
            chatService.saveMessage(message);

            // 성공 시 응답 객체 생성 및 발송
            SocketResponseDTO<ChatMessageDTO> response = SocketResponseDTO.message(message.getChatroomId(), message);
            messagingTemplate.convertAndSend("/sub/chatrooms/" + message.getChatroomId(), response);

        } catch (Exception e) {
            log.error("메시지 처리 실패: {}", e.getMessage(), e);
            throw new GlobalException(ChatErrorCode.ERROR_SAVING_MESSAGE);
        }
    }
}