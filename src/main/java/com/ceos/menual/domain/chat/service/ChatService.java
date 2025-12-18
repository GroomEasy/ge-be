package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.repository.MessageRepository;
import com.ceos.menual.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;

    @Transactional
    public void saveMessage(ChatMessageDTO messageDto) {
        // DTO -> Entity 변환
        Message message = Message.create(messageDto);

        // DB 저장
        messageRepository.save(message);
    }
}