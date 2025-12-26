package com.ceos.menual.domain.chat.repository;

import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.enums.ChatroomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    // 해당 상담(consultationId)에서 해당 타입(type)의 방이 이미 있는지 확인
    Optional<Chatroom> findByConsultationIdAndChatroomType(Long consultationId, ChatroomType type);

    // memberId나 expertId에 해당하는 id에 해당하는 채팅방 리스트 조회
    @Query("SELECT DISTINCT c FROM Chatroom c " +
            "LEFT JOIN FETCH c.member m " +
            "LEFT JOIN FETCH c.expert e " +
            "LEFT JOIN FETCH e.expertProfile ep " +
            "WHERE c.member.id = :memberId OR c.expert.id = :memberId")
    List<Chatroom> findAllByParticipantId(@Param("memberId") Long memberId);
}