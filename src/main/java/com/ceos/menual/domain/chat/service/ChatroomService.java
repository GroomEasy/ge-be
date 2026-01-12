package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatMessageResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomListResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.dto.response.MessageReadResponseDTO;
import com.ceos.menual.domain.chat.exception.ChatErrorCode;
import com.ceos.menual.domain.chat.repository.ChatMessageRepository;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.consultation.repository.ConsultationScheduleRepository;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.entity.enums.ChatroomType;
import com.ceos.menual.entity.enums.MessageType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ConsultationRepository consultationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;


    /**
     * 채팅방 생성 메서드
     */
    @Transactional
    public ChatroomResponseDTO createChatroom(Long memberId, Long consultationId, ChatroomCreateRequestDTO request) {

        // 상담 정보 조회
        Consultation consultation = consultationRepository.findByIdWithAllRelations(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        Long expertId = consultation.getExpertProfile().getUser().getId();
        Long generalId = consultation.getGeneralProfile().getUser().getId();

        // 권한 검증
        if (!memberId.equals(expertId) && !memberId.equals(generalId)) {
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_ACCESS_DENIED);
        }

        // 데이터 추출
        ExpertProfile expertProfile = consultation.getExpertProfile();
        GeneralProfile generalProfile = consultation.getGeneralProfile();

        User expertUser = expertProfile.getUser();
        User memberUser = generalProfile.getUser();

        ChatroomType chatroomType = request.getChatroomType();

        // 같은 타입의 활성 채팅방이 있는지 확인
        Optional<Chatroom> activeChatroom = chatroomRepository
                .findActiveChatroomByMemberAndExpertAndType(
                        memberUser.getId(),
                        expertUser.getId(),
                        chatroomType
                );

        if (activeChatroom.isPresent()) {
            log.warn("이미 진행 중인 {} 상담이 있습니다 - chatroomId: {}, memberId: {}, expertId: {}",
                    chatroomType, activeChatroom.get().getId(), memberUser.getId(), expertUser.getId());
            throw new GlobalException(ChatErrorCode.CONSULTATION_ALREADY_IN_PROGRESS);
        }

        // 같은 타입의 비활성 채팅방이 있는지 확인
        Optional<Chatroom> inactiveChatroom = chatroomRepository
                .findLatestInactiveChatroomByMemberAndExpertAndType(
                        memberUser.getId(),
                        expertUser.getId(),
                        chatroomType
                );

        Chatroom chatroom;

        if (inactiveChatroom.isPresent()) {
            // 기존 채팅방 재활성화
            chatroom = inactiveChatroom.get();
            chatroom.activate();
            chatroom.updateConsultation(consultationId);

            log.info("기존 {} 채팅방 재활성화 - chatroomId: {}, consultationId: {}",
                    chatroomType, chatroom.getId(), consultationId);
        } else {
            // 새로운 채팅방 생성
            log.info("새 {} 채팅방 생성 - consultationId: {}", chatroomType, consultationId);

            chatroom = Chatroom.builder()
                    .consultationId(consultationId)
                    .chatroomType(chatroomType)
                    .member(memberUser)
                    .expert(expertUser)
                    .build();

            chatroom = chatroomRepository.save(chatroom);

            log.info("채팅방 생성 완료 - chatroomId: {}, consultationId: {}, type: {}",
                    chatroom.getId(), consultationId, chatroomType);
        }

        // 응답 DTO 생성
        ChatroomResponseDTO.ExpertInfo expertInfo = ChatroomResponseDTO.ExpertInfo.builder()
                .userId(expertId)
                .nickname(expertUser.getNickname())
                .category(expertProfile.getCategory().getDescription())
                .build();

        ChatroomResponseDTO.MemberInfo memberInfo = ChatroomResponseDTO.MemberInfo.builder()
                .userId(memberUser.getId())
                .nickname(memberUser.getNickname())
                .build();

        return ChatroomResponseDTO.of(chatroom, expertInfo, memberInfo);
    }

    /**
     * 채팅방 목록(리스트) 조회 메서드
     */
    public List<ChatroomListResponseDTO> getChatroomList(Long memberId) {

        List<Chatroom> chatrooms = chatroomRepository.findAllByParticipantId(memberId);

        if (chatrooms.isEmpty()) {
            return List.of();
        }

        return buildChatroomListResponse(chatrooms, memberId);
    }

    /**
     * 특정 타입의 채팅방 목록 조회
     */
    public List<ChatroomListResponseDTO> getChatroomListByType(Long memberId, ChatroomType chatroomType) {

        // 특정 타입의 채팅방만 조회
        List<Chatroom> chatrooms = chatroomRepository.findAllByParticipantIdAndType(memberId, chatroomType);

        if (chatrooms.isEmpty()) {
            return List.of();
        }

        return buildChatroomListResponse(chatrooms, memberId);
    }

    /**
     * 안읽은 메시지가 있는 채팅방 목록 조회
     */
    public List<ChatroomListResponseDTO> getChatroomsWithUnreadMessages(Long memberId) {

        // 모든 채팅방 조회
        List<Chatroom> chatrooms = chatroomRepository.findAllByParticipantId(memberId);

        if (chatrooms.isEmpty()) {
            return List.of();
        }

        // 채팅방 ID 리스트 추출
        List<Long> chatroomIds = chatrooms.stream()
                .map(Chatroom::getId)
                .collect(Collectors.toList());

        // 읽지 않은 메시지 수 조회
        Map<Long, Long> unreadCountMap = chatMessageRepository
                .countUnreadMessagesByChatroomIds(chatroomIds, memberId);

        // 안읽은 메시지가 있는 채팅방만 필터링
        List<Chatroom> unreadChatrooms = chatrooms.stream()
                .filter(chatroom -> unreadCountMap.getOrDefault(chatroom.getId(), 0L) > 0)
                .collect(Collectors.toList());

        if (unreadChatrooms.isEmpty()) {
            return List.of();
        }

        return buildChatroomListResponse(unreadChatrooms, memberId);
    }

    /**
     * 채팅방 리스트를 DTO로 변환하는 공통 메서드
     */
    private List<ChatroomListResponseDTO> buildChatroomListResponse(List<Chatroom> chatrooms, Long memberId) {

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

        // 마지막 메시지 표시 텍스트
        String lastMessageText = null;
        MessageType lastMessageType = null;

        String memberNickname = chatroom.getMember().getNickname();

        if (lastMessage != null) {
            lastMessageType = lastMessage.getMessageType();

            switch (lastMessage.getMessageType()) {
                case TEXT:
                    lastMessageText = lastMessage.getContent();
                    break;
                case IMAGE:
                    lastMessageText = "사진을 보냈습니다";
                    break;
                case MIXED:
                    lastMessageText = lastMessage.getContent();  // 텍스트만 표시
                    break;
                case CONCERN:
                    lastMessageText = memberNickname + "님을 위한 고민지가 도착했어요.";
                    break;
                case SOLUTION:
                    lastMessageText = memberNickname + "님을 위한 솔루션지가 도착했어요.";
                    break;
                case SYSTEM:
                    lastMessageText = lastMessage.getContent();
                    break;
            }
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
                // 메시지 정보
                .lastMessage(lastMessageText)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .lastMessageType(lastMessageType)
                .unreadCount(unreadCount)
                .createdAt(chatroom.getCreatedAt())
                .build();
    }

    /**
     * 특정 채팅방의 메시지 목록 조회
     */
    @Transactional
    public List<ChatMessageResponseDTO> getChatroomMessages(Long memberId, Long chatroomId) {
        // 채팅방 존재 여부 및 권한 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        // 채팅방 참여자인지 확인
        if (!chatroom.getMember().getId().equals(memberId) &&
                !chatroom.getExpert().getId().equals(memberId)) {
            throw new GlobalException(ChatErrorCode.CHATROOM_ACCESS_DENIED);
        }

        // 메시지 목록 조회 (오래된 순)
        List<Message> messages = chatMessageRepository.findByChatroomIdOrderByCreatedAtAsc(chatroomId);

        if (messages.isEmpty()) {
            return List.of();
        }

        // 발신자 정보를 한 번에 조회 (N+1 문제 방지)
        Set<Long> senderIds = messages.stream()
                .map(Message::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // DTO 변환
        List<ChatMessageResponseDTO> responseDTOs = messages.stream()
                .map(message -> {
                    User sender = userMap.get(message.getSenderId());
                    String senderNickname = sender != null ? sender.getNickname() : "알 수 없음";

                    // senderRole 결정: EXPERT 또는 MEMBER
                    String senderRole = determineSenderRole(chatroom, message.getSenderId());

                    return ChatMessageResponseDTO.from(message, senderNickname, senderRole);
                })
                .collect(Collectors.toList());

        // 읽지 않은 메시지 읽음 처리 (현재 사용자가 받은 메시지만)
        markMessagesAsRead(messages, memberId);

        return responseDTOs;
    }

    /**
     * 채팅방의 모든 메시지를 읽음 처리
     */
    @Transactional
    public MessageReadResponseDTO markAllMessagesAsRead(Long memberId, Long chatroomId) {
        // 채팅방 존재 여부 및 권한 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        if (!chatroom.getMember().getId().equals(memberId) &&
                !chatroom.getExpert().getId().equals(memberId)) {
            throw new GlobalException(ChatErrorCode.CHATROOM_ACCESS_DENIED);
        }

        // 채팅방의 모든 읽지 않은 메시지 읽음 처리 (내가 보낸 메시지 제외)
        int readCount = chatMessageRepository.markAllMessagesAsReadInChatroom(chatroomId, memberId);

        return MessageReadResponseDTO.of(readCount);
    }

    /**
     * 발신자의 역할 결정 (EXPERT or MEMBER)
     */
    private String determineSenderRole(Chatroom chatroom, Long senderId) {
        if (chatroom.getExpert().getId().equals(senderId)) {
            return "EXPERT";
        } else if (chatroom.getMember().getId().equals(senderId)) {
            return "MEMBER";
        }
        return "UNKNOWN";
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markMessagesAsRead(List<Message> messages, Long memberId) {
        messages.stream()
                .filter(message -> !message.getSenderId().equals(memberId))
                .filter(message -> !message.isRead())
                .forEach(Message::markAsRead);
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