package com.ceos.menual.domain.chat.dto.request;

import com.ceos.menual.entity.enums.ChatroomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅방 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatroomCreateRequestDTO {

    @Schema(description = "상담 ID", example = "1")
    @NotNull(message = "상담 ID는 필수입니다")
    private Long consultationId;

    @Schema(description = "채팅방 타입 (MESSAGE/VIDEO)", example = "MESSAGE")
    @NotNull(message = "채팅방 타입은 필수입니다")
    private ChatroomType chatroomType;
}