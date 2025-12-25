package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomListResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ConsultationRepository consultationRepository;

    /*
     * 채팅방 생성 메서드
     */
    @Transactional
    public ChatroomResponseDTO createChatroom(Long consultationId, ChatroomCreateRequestDTO request) {

        // 상담 정보 조회
        Consultation consultation = consultationRepository.findByIdWithAllRelations(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // 데이터 추출
        ExpertProfile expertProfile = consultation.getExpertProfile();
        GeneralProfile generalProfile = consultation.getGeneralProfile();

        User expertUser = expertProfile.getUser();
        User memberUser = generalProfile.getUser();

        String categoryName = expertProfile.getCategory().getDescription();

        Long expertId = expertUser.getId();
        Long memberId = memberUser.getId();

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
                .userId(memberId)
                .nickname(memberUser.getNickname())
                .build();

        return ChatroomResponseDTO.of(chatroom, expertInfo, memberInfo);
    }

    /*
     * 채팅방 목록(리스트) 조회 메서드
     */
//    public List<ChatroomListResponseDTO> getChatroomList(Long memberId) {
//
//        // memberId로 chatroom 목록 가져오기
//        List<Chatroom> chatrooms = chatroomRepository.findAllByParticipantId(memberId);
//
//        // 가져온 방들을 DTO로 변환하고 최신순으로 정렬
//        return chatrooms.stream()
//                .map(chatroom -> toChatroomListResponseDTO(chatroom, memberId)) // 아래 헬퍼 메서드 호출
//                .sorted((c1, c2) -> {
//                    // 정렬 로직: 마지막 메시지 시간(lastMessageAt) 내림차순
//                    if (c1.getLastMessageAt() == null) return 1;  // 메시지 없으면 맨 뒤로
//                    if (c2.getLastMessageAt() == null) return -1;
//                    return c2.getLastMessageAt().compareTo(c1.getLastMessageAt());
//                })
//                .collect(Collectors.toList());
//    }

}