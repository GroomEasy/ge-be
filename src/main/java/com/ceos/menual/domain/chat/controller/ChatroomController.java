package com.ceos.menual.domain.chat.controller;

import com.ceos.menual.domain.chat.dto.request.ChatroomCreateRequestDTO;
import com.ceos.menual.domain.chat.dto.response.ChatMessageResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomListResponseDTO;
import com.ceos.menual.domain.chat.dto.response.ChatroomResponseDTO;
import com.ceos.menual.domain.chat.dto.response.MessageReadResponseDTO;
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

    @Operation(summary = "채팅방 메시지 조회", description = "특정 채팅방의 모든 메시지를 시간순으로 조회합니다.")
    @GetMapping("/{chatroomId}/messages")
    public CommonResponse<List<ChatMessageResponseDTO>> getChatroomMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatroomId
    ) {
        List<ChatMessageResponseDTO> response = chatroomService.getChatroomMessages(memberId, chatroomId);
        return CommonResponse.success(response);
    }

    @Operation(summary = "채팅방 메시지 읽음 처리", description = "채팅방의 읽지 않은 모든 메시지를 읽음 상태로 변경합니다.")
    @PatchMapping("/{chatroomId}/messages/read")
    public CommonResponse<MessageReadResponseDTO> markMessagesAsRead(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long chatroomId
    ) {
        MessageReadResponseDTO response = chatroomService.markAllMessagesAsRead(memberId, chatroomId);
        return CommonResponse.success(response);
    }
}