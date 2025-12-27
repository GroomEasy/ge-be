package com.ceos.menual.domain.chat.repository;

import com.ceos.menual.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ChatMessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatroomIdOrderByCreatedAtAsc(Long chatroomId);

    /**
     * 특정 채팅방의 마지막 메시지 조회
     */
    Optional<Message> findFirstByChatroomIdOrderByCreatedAtDesc(Long chatroomId);

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     * (Message 엔티티에 chatroomId, senderId, isRead 필드가 있다고 가정)
     */
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.chatroomId = :chatroomId " +
            "AND m.senderId != :memberId " +
            "AND m.isRead = false")
    Long countUnreadMessages(@Param("chatroomId") Long chatroomId,
                             @Param("memberId") Long memberId);

    /**
     * 여러 채팅방의 마지막 메시지를 한 번에 조회 (배치 쿼리)
     * - Message 엔티티의 chatroomId 필드 사용
     */
    @Query("SELECT m FROM Message m " +
            "WHERE m.id IN (" +
            "  SELECT MAX(m2.id) FROM Message m2 " +
            "  WHERE m2.chatroomId IN :chatroomIds " +
            "  GROUP BY m2.chatroomId" +
            ")")
    List<Message> findLastMessagesByChatroomIdsAsList(@Param("chatroomIds") List<Long> chatroomIds);

    /**
     * List를 Map으로 변환하는 default 메서드
     * Key: chatroomId, Value: Message
     */
    default Map<Long, Message> findLastMessagesByChatroomIds(List<Long> chatroomIds) {
        return findLastMessagesByChatroomIdsAsList(chatroomIds).stream()
                .collect(Collectors.toMap(
                        Message::getChatroomId, // Message 엔티티의 getChatroomId() 호출
                        message -> message
                ));
    }

    /**
     * 여러 채팅방의 읽지 않은 메시지 수를 한 번에 조회 (배치 쿼리)
     */
    @Query("SELECT m.chatroomId, COUNT(m) FROM Message m " +
            "WHERE m.chatroomId IN :chatroomIds " +
            "AND m.senderId != :memberId " +
            "AND m.isRead = false " +
            "GROUP BY m.chatroomId")
    List<Object[]> countUnreadMessagesByChatroomIdsAsList(@Param("chatroomIds") List<Long> chatroomIds,
                                                          @Param("memberId") Long memberId);

    /**
     * Object[] 배열을 Map으로 변환하는 default 메서드
     * Key: chatroomId, Value: count
     */
    default Map<Long, Long> countUnreadMessagesByChatroomIds(List<Long> chatroomIds, Long memberId) {
        return countUnreadMessagesByChatroomIdsAsList(chatroomIds, memberId).stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],  // chatroomId
                        arr -> (Long) arr[1]   // count
                ));
    }

    /**
     * 특정 채팅방의 모든 읽지 않은 메시지를 읽음 처리
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
            "WHERE m.chatroomId = :chatroomId " +
            "AND m.senderId != :memberId " +
            "AND m.isRead = false")
    int markAllMessagesAsReadInChatroom(@Param("chatroomId") Long chatroomId, @Param("memberId") Long memberId);
}