package com.ceos.menual.domain.chat.dto.response;

import com.ceos.menual.entity.enums.ChatroomType;
import com.ceos.menual.entity.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatroomListResponseDTO {

    @Schema(description = "채팅방 ID", example = "100")
    private Long chatroomId;

    @Schema(description = "연관된 상담/컨설팅 ID", example = "55")
    private Long consultationId;

    @Schema(description = "채팅방 타입 (MESSAGE, VIDEO, NOTICE)", example = "MESSAGE")
    private ChatroomType chatroomType;

    @Schema(description = "상대방 ID (회원 또는 멘토 ID)", example = "3")
    private Long opponentId;

    @Schema(description = "상대방 닉네임", example = "최영인")
    private String opponentNickname;

    @Schema(description = "전문가 카테고리 이름 (헤어, 패션 등)", example = "헤어")
    private String expertCategory;

    @Schema(description = "상대방 프로필 이미지 URL", example = "https://s3.../profile.jpg")
    private String opponentProfileImage;

    @Schema(description = "마지막 메시지 내용", example = "감사합니다.")
    private String lastMessage;

    @Schema(description = "마지막 메시지 전송 시간", example = "2025-12-25T15:30:00")
    private LocalDateTime lastMessageAt;

    @Schema(description = "안 읽은 메시지 개수", example = "3")
    private Long unreadCount;

    private LocalDateTime createdAt;
}