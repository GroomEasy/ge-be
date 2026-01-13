package com.ceos.menual.domain.chat.dto.response;

import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ChatroomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatroomResponseDTO {

    @Schema(description = "채팅방 ID", example = "1")
    private Long chatroomId;

    @Schema(description = "상담 ID", example = "1")
    private Long consultationId;

    @Schema(description = "채팅방 유형 (MESSAGE, VIDEO, ADMIN)", example = "MESSAGE")
    private ChatroomType chatroomType;

    @Schema(description = "전문가 정보")
    private ExpertInfo expert;

    @Schema(description = "일반 회원 정보")
    private MemberInfo member;

    @Schema(description = "채팅방 생성 시각", example = "2025-12-26T22:30:00", type = "string", format = "date-time")
    private LocalDateTime createdAt;

    // 내부 클래스: 전문가 정보
    @Getter
    @Builder
    @Schema(description = "전문가 정보 DTO")
    public static class ExpertInfo {

        @Schema(description = "전문가 userId", example = "10")
        private Long userId;

        @Schema(description = "전문가 닉네임", example = "헤어마스터")
        private String nickname;

        @Schema(description = "전문가 카테고리", example = "헤어")
        private String category;
    }

    // 내부 클래스: 일반 회원 정보
    @Getter
    @Builder
    @Schema(description = "일반 회원 정보 DTO")
    public static class MemberInfo {

        @Schema(description = "일반 회원 userId", example = "4")
        private Long userId;

        @Schema(description = "일반 회원 닉네임", example = "지원")
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