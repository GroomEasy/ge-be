package com.ceos.menual.domain.chat.controller;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.service.ChatroomService;
import com.ceos.menual.domain.common.dto.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/room")
public class ChatroomController {

    private final ChatroomService chatroomService;

    @Operation(summary = "채팅방 생성 (또는 기존 방 조회)", description = "상대방(expertId)과 1:1 채팅방을 생성하거나 이미 존재하면 정보를 반환합니다.")
    @PostMapping
    public CommonResponse<ChatroomResponseDTO> createRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ChatroomCreateRequestDTO request
    ) {

        ChatroomResponseDTO response = chatroomService.createChatroom(memberId, request);

        return CommonResponse.success(response);
    }
}