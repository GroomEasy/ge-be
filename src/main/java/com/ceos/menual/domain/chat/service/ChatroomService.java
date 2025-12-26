package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomListResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.repository.ChatMessageRepository;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ConsultationRepository consultationRepository;
    private final ChatMessageRepository chatMessageRepository;


    /*
     * 채팅방 생성 메서드
     */
    @Transactional
    public ChatroomResponseDTO createChatroom(Long memberId, Long consultationId, ChatroomCreateRequestDTO request) {

        // 상담 정보 조회
        Consultation consultation = consultationRepository.findByIdWithAllRelations(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        Long expertId = consultation.getExpertProfile().getUser().getId();
        Long generalId = consultation.getGeneralProfile().getUser().getId();

        if (!memberId.equals(expertId) && !memberId.equals(generalId)) {
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_ACCESS_DENIED);
        }

        // 데이터 추출
        ExpertProfile expertProfile = consultation.getExpertProfile();
        GeneralProfile generalProfile = consultation.getGeneralProfile();

        User expertUser = expertProfile.getUser();
        User memberUser = generalProfile.getUser();

        String categoryName = expertProfile.getCategory().getDescription();

        // 채팅방 존재 여부 확인 및 생성
        Chatroom chatroom = chatroomRepository.findByConsultationIdAndChatroomType(consultationId, request.getChatroomType())
                .orElseGet(() -> {
                    Chatroom newRoom = Chatroom.builder()
                            .consultationId(consultationId)
                            .chatroomType(request.getChatroomType())
                            .member(memberUser)
                            .expert(expertUser)
                            .build();
                    return chatroomRepository.save(newRoom);
                });

        // 응답 DTO 생성
        ChatroomResponseDTO.ExpertInfo expertInfo = ChatroomResponseDTO.ExpertInfo.builder()
                .userId(expertId)
                .nickname(expertUser.getNickname())
                .categoryName(categoryName) // 조회한 카테고리 이름
                .build();

        ChatroomResponseDTO.MemberInfo memberInfo = ChatroomResponseDTO.MemberInfo.builder()
                .userId(memberUser.getId())
                .nickname(memberUser.getNickname())
                .build();

        return ChatroomResponseDTO.of(chatroom, expertInfo, memberInfo);
    }

    /*
     * 채팅방 목록(리스트) 조회 메서드
     */
    public List<ChatroomListResponseDTO> getChatroomList(Long memberId) {

        // memberId로 chatroom 목록 가져오기
        List<Chatroom> chatrooms = chatroomRepository.findAllByParticipantId(memberId);

        if (chatrooms.isEmpty()) {
            return List.of();
        }

        // 채팅방 ID 리스트 추출
        List<Long> chatroomIds = chatrooms.stream()
                .map(Chatroom::getId)
                .collect(Collectors.toList());

        // 모든 채팅방의 마지막 메시지를 한 번에 조회
        Map<Long, Message> lastMessageMap = chatMessageRepository
                .findLastMessagesByChatroomIds(chatroomIds);

        // 모든 채팅방의 읽지 않은 메시지 수를 한 번에 조회
        Map<Long, Long> unreadCountMap = chatMessageRepository
                .countUnreadMessagesByChatroomIds(chatroomIds, memberId);

        // DTO 변환 및 정렬
        return chatrooms.stream()
                .map(chatroom -> toChatroomListResponseDTO(
                        chatroom,
                        memberId,
                        lastMessageMap.get(chatroom.getId()),
                        unreadCountMap.getOrDefault(chatroom.getId(), 0L)
                ))
                .sorted(Comparator.comparing(
                        ChatroomListResponseDTO::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Chatroom 엔티티를 DTO로 변환
     * @param chatroom 채팅방 엔티티
     * @param memberId 현재 사용자 ID
     * @param lastMessage 마지막 메시지
     * @param unreadCount 읽지 않은 메시지 수
     */
    private ChatroomListResponseDTO toChatroomListResponseDTO(
            Chatroom chatroom,
            Long memberId,
            Message lastMessage,
            Long unreadCount
    ) {
        // 상대방 찾기
        User opponent = findOpponent(chatroom, memberId);

        // 전문가 카테고리 추출
        String expertCategoryName = null;
        if (chatroom.getExpert() != null) {
            expertCategoryName = chatroom.getExpert().getExpertProfile().getCategory().getDescription();
        }

        // DTO 빌드
        return ChatroomListResponseDTO.builder()
                .chatroomId(chatroom.getId())
                .consultationId(chatroom.getConsultationId())
                .chatroomType(chatroom.getChatroomType())
                // 상대방 정보
                .opponentId(opponent.getId())
                .opponentNickname(opponent.getNickname())
                .opponentProfileImage(opponent.getProfileImage())
                .expertCategory(expertCategoryName)
                // 메시지 정보 (파라미터로 받은 값 사용)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .unreadCount(unreadCount)
                .createdAt(chatroom.getCreatedAt())
                .build();
    }

    /**
     * 채팅방에서 상대방 찾기
     */
    private User findOpponent(Chatroom chatroom, Long memberId) {
        if (chatroom.getMember().getId().equals(memberId)) {
            return chatroom.getExpert();
        } else {
            return chatroom.getMember();
        }
    }

}