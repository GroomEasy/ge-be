package com.ceos.menual.domain.chat.dto.response;

import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.enums.ChatroomType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatroomResponseDTO {

    private Long chatroomId;
    private Long consultationId;
    private ChatroomType chatroomType;
    private ExpertInfo expert;
    private MemberInfo member;
    private LocalDateTime createdAt;

    // 내부 클래스: 전문가 정보
    @Getter
    @Builder
    public static class ExpertInfo {
        private Long userId;
        private String nickname;
        private String categoryName;
    }

    // 내부 클래스: 일반 회원 정보
    @Getter
    @Builder
    public static class MemberInfo {
        private Long userId;
        private String nickname;
    }

    // Entity -> DTO 변환
    public static ChatroomResponseDTO of(Chatroom chatroom, ExpertInfo expertInfo, MemberInfo memberInfo) {
        return ChatroomResponseDTO.builder()
                .chatroomId(chatroom.getId())
                .consultationId(chatroom.getConsultationId())
                .chatroomType(chatroom.getChatroomType())
                .expert(expertInfo)
                .member(memberInfo)
                .createdAt(chatroom.getCreatedAt())
                .build();
    }
}