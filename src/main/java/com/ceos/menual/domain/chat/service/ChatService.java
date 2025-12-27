package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.repository.ChatMessageRepository;
import com.ceos.menual.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;

    @Transactional
    public void saveMessage(ChatMessageDTO messageDto) {
        // DTO -> Entity 변환
        Message message = Message.create(messageDto);


        //TODO: Redis 도입 후 스케줄러로 DB에 주기적 저장

        // DB 저장
        messageRepository.save(message);
    }
}