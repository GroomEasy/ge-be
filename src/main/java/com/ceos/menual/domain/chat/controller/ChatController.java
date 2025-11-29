package com.ceos.menual.domain.chat.controller;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.dto.response.SocketResponse;
import com.ceos.menual.domain.chat.service.ChatService;
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
    // (Config에서 prefix를 /pub으로 했으므로 여기선 /chat/message만 씀)
    @MessageMapping("/chat/message")
    public void message(ChatMessageDTO message, SimpMessageHeaderAccessor headerAccessor) {

        // 세션에서 userId(senderId) 추출
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        Long senderId = (Long) sessionAttributes.get("userId");

        if (senderId == null) {
            log.error("세션에 userId가 없습니다. (인증 실패)");
            return;
        }

        // DTO에 보낸 사람 ID 주입
        message.setSenderId(senderId);

        // 메시지 타입에 따른 시스템 로직
         if (ChatMessageDTO.MessageType.SYSTEM.equals(message.getMessageType())) {
             message.setContent("시스템 알림: " + message.getContent());
         }

        // DB에 메시지 저장
        chatService.saveMessage(message);

        SocketResponse<ChatMessageDTO> response = SocketResponse.message(message.getChatroomId(), message);

        // 구독자들에게 전송 (/sub/chatrooms/{id})
        messagingTemplate.convertAndSend("/sub/chatrooms/" + message.getChatroomId(), response);
    }
}