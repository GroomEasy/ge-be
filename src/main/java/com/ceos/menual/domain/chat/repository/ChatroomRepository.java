package com.ceos.menual.domain.chat.repository;

import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.enums.ChatroomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    // 해당 상담(consultationId)에서 해당 타입(type)의 방이 이미 있는지 확인
    Optional<Chatroom> findByConsultationIdAndChatroomType(Long consultationId, ChatroomType type);
}