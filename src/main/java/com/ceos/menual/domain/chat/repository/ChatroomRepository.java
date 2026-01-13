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

    /**
     * 특정 타입의 채팅방 목록 조회
     */
    @Query("SELECT DISTINCT c FROM Chatroom c " +
            "LEFT JOIN FETCH c.member m " +
            "LEFT JOIN FETCH c.expert e " +
            "LEFT JOIN FETCH e.expertProfile ep " +
            "WHERE (c.member.id = :memberId OR c.expert.id = :memberId) " +
            "AND c.chatroomType = :chatroomType")
    List<Chatroom> findAllByParticipantIdAndType(@Param("memberId") Long memberId,
                                                 @Param("chatroomType") ChatroomType chatroomType);

    /**
     * 회원과 전문가 간의 특정 타입의 활성 채팅방 조회
     */
    @Query("SELECT c FROM Chatroom c " +
            "WHERE c.member.id = :memberId " +
            "AND c.expert.id = :expertId " +
            "AND c.chatroomType = :chatroomType " +
            "AND c.isActive = true")
    Optional<Chatroom> findActiveChatroomByMemberAndExpertAndType(
            @Param("memberId") Long memberId,
            @Param("expertId") Long expertId,
            @Param("chatroomType") ChatroomType chatroomType
    );

    /**
     * 회원과 전문가 간의 특정 타입의 비활성 채팅방 조회 (가장 최근 것)
     */
    @Query("SELECT c FROM Chatroom c " +
            "WHERE c.member.id = :memberId " +
            "AND c.expert.id = :expertId " +
            "AND c.chatroomType = :chatroomType " +
            "AND c.isActive = false " +
            "ORDER BY c.createdAt DESC")
    List<Chatroom> findInactiveChatroomsByMemberAndExpertAndType(
            @Param("memberId") Long memberId,
            @Param("expertId") Long expertId,
            @Param("chatroomType") ChatroomType chatroomType
    );

    /**
     * 관리자-전문가 ADMIN 타입 채팅방 조회
     */
    @Query("SELECT c FROM Chatroom c " +
            "WHERE c.member.id = :adminId " +
            "AND c.expert.id = :expertId " +
            "AND c.chatroomType = 'ADMIN' " +
            "AND c.isActive = true")
    Optional<Chatroom> findActiveAdminChatroomByAdminAndExpert(
            @Param("adminId") Long adminId,
            @Param("expertId") Long expertId
    );
}