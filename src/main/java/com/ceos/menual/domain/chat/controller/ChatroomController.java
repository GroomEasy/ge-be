package com.ceos.menual.domain.chat.controller;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomListResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.service.ChatroomService;
import com.ceos.menual.domain.common.dto.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/room")
public class ChatroomController {

    private final ChatroomService chatroomService;

    @Operation(summary = "채팅방 생성", description = "상대방과 1:1 채팅방을 생성합니다.")
    @PostMapping
    public CommonResponse<ChatroomResponseDTO> createRoom(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ChatroomCreateRequestDTO request
    ) {
        ChatroomResponseDTO response = chatroomService.createChatroom(memberId, request.getConsultationId(), request);
        return CommonResponse.success(response);
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 참여 중인 채팅방 목록을 최신 메시지 순으로 조회합니다.")
    @GetMapping
    public CommonResponse<List<ChatroomListResponseDTO>> getChatroomList(
            @AuthenticationPrincipal Long memberId
    ) {
        List<ChatroomListResponseDTO> response = chatroomService.getChatroomList(memberId);
        return CommonResponse.success(response);
    }
}