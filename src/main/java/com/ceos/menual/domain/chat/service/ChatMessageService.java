package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.dto.response.SocketResponseDTO;
import com.ceos.menual.domain.chat.exception.ChatErrorCode;
import com.ceos.menual.domain.chat.repository.ChatMessageRepository;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Message;
import com.ceos.menual.entity.enums.MessageType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatroomRepository chatroomRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 고민지 메시지 자동 전송
     */
    @Transactional
    public void sendConcernMessage(Long chatroomId, Long memberId, String concernsJson, Long reservationId) {
        log.info("고민지 메시지 전송 시작 - chatroomId: {}, memberId: {}", chatroomId, memberId);

        // 채팅방 존재 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        // 권한 확인
        if (!chatroom.getMember().getId().equals(memberId)) {
            throw new GlobalException(ChatErrorCode.CHATROOM_ACCESS_DENIED);
        }

        // 고민지 메시지 생성 및 저장
        Message concernMessage = Message.builder()
                .chatroomId(chatroomId)
                .senderId(memberId)
                .content(concernsJson)
                .messageType(MessageType.CONCERN)
                .relatedId(reservationId)
                .build();

        Message savedMessage = chatMessageRepository.save(concernMessage);

        // WebSocket으로 실시간 전송
        ChatMessageDTO messageDTO = ChatMessageDTO.builder()
                .chatroomId(chatroomId)
                .senderId(memberId)
                .content(concernsJson)
                .messageType(MessageType.CONCERN)
                .relatedId(reservationId)
                .build();

        SocketResponseDTO<ChatMessageDTO> response = SocketResponseDTO.message(chatroomId, messageDTO);
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatroomId, response);

        log.info("고민지 메시지 전송 완료 - messageId: {}, chatroomId: {}",
                savedMessage.getId(), chatroomId);
    }


}