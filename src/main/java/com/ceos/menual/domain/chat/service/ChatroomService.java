package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.*;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomService {

    private final ChatroomRepository chatroomRepository;
    private final ConsultationRepository consultationRepository;

    @Transactional
    public ChatroomResponseDTO createChatroom(Long consultationId, ChatroomCreateRequestDTO request) {

        // 상담 정보 조회 (모든 연관관계 한 번에 로딩)
        Consultation consultation = consultationRepository.findByIdWithAllRelations(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // 데이터 추출
        ExpertProfile expertProfile = consultation.getExpertProfile();
        GeneralProfile generalProfile = consultation.getGeneralProfile();

        User expertUser = expertProfile.getUser();
        User memberUser = generalProfile.getUser();

        // Category 엔티티에 'name' 필드가 있다고 가정 (없으면 필드명에 맞게 수정: getName(), getCategoryName() 등)
        String categoryName = expertProfile.getCategory().getName();

        Long expertId = expertUser.getId();
        Long memberId = memberUser.getId();

        // 채팅방 존재 여부 확인 및 생성 (Find or Create)
        Chatroom chatroom = chatroomRepository.findByConsultationIdAndChatroomType(consultationId, request.getChatroomType())
                .orElseGet(() -> {
                    Chatroom newRoom = Chatroom.builder()
                            .consultationId(consultationId)
                            .chatroomType(request.getChatroomType())
                            .memberId(memberId)
                            .expertId(expertId)
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
}