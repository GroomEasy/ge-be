package com.ceos.menual.domain.chat.service;

import com.ceos.menual.domain.chat.dto.request.ChatMessageDTO;
import com.ceos.menual.domain.chat.dto.response.SocketResponseDTO;
import com.ceos.menual.domain.chat.exception.ChatErrorCode;
import com.ceos.menual.domain.chat.repository.ChatMessageRepository;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Message;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.ConsultationType;
import com.ceos.menual.entity.enums.MessageType;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatroomRepository chatroomRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;

    /**
     * 고민지 메시지 자동 전송
     */
    @Transactional
    public void sendConcernMessage(Long chatroomId, Long memberId, Long reservationId) {
        log.info("고민지 메시지 전송 시작 - chatroomId: {}, memberId: {}", chatroomId, memberId);

        // 채팅방 존재 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        // 권한 확인
        if (!chatroom.getMember().getId().equals(memberId)) {
            throw new GlobalException(ChatErrorCode.CHATROOM_ACCESS_DENIED);
        }

        // 고민지 메시지 생성 및 저장
        Message concernMessage = Message.builder()
                .chatroomId(chatroomId)
                .senderId(memberId)
                .messageType(MessageType.CONCERN)
                .relatedId(reservationId)
                .build();

        Message savedMessage = chatMessageRepository.save(concernMessage);

        // WebSocket으로 실시간 전송
        ChatMessageDTO messageDTO = savedMessage.toDTO();


        SocketResponseDTO<ChatMessageDTO> response = SocketResponseDTO.message(chatroomId, messageDTO);
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatroomId, response);

        log.info("고민지 메시지 전송 완료 - messageId: {}, chatroomId: {}",
                savedMessage.getId(), chatroomId);
    }

    /**
     * 솔루션지 메시지 전송
     */
    @Transactional
    public void sendSolutionMessage(Long chatroomId, Long expertId, String memberNickname, Long consultationId) {
        log.info("솔루션지 메시지 전송 시작 - chatroomId: {}, expertId: {}", chatroomId, expertId);

        // 채팅방 존재 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        // 권한 확인
        if (!chatroom.getExpert().getId().equals(expertId)) {
            throw new GlobalException(ChatErrorCode.CHATROOM_ACCESS_DENIED);
        }

        // 솔루션지 알림 메시지 생성
        String solutionContent = memberNickname + "님을 위한 솔루션지가 도착했습니다.";

        // 솔루션 메시지 생성 및 저장
        Message solutionMessage = Message.builder()
                .chatroomId(chatroomId)
                .senderId(expertId)
                .content(solutionContent)
                .messageType(MessageType.SOLUTION)
                .relatedId(consultationId)
                .build();

        Message savedMessage = chatMessageRepository.save(solutionMessage);

        // WebSocket으로 실시간 전송
        ChatMessageDTO messageDTO = savedMessage.toDTO();

        SocketResponseDTO<ChatMessageDTO> response = SocketResponseDTO.message(chatroomId, messageDTO);
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatroomId, response);

        log.info("솔루션지 메시지 전송 완료 - messageId: {}, chatroomId: {}",
                savedMessage.getId(), chatroomId);
    }

    /**
     *  관리자 시스템 메시지 전송 (상담 예약 알림)
     */
    @Transactional
    public void sendReservationNotificationToExpert(
            Long chatroomId,
            Long adminUserId,
            String memberName,
            LocalDateTime scheduledDateTime,
            ConsultationType consultationType,
            Long consultationId
    ) {
        log.info("상담 예약 알림 전송 시작 - chatroomId: {}, consultationType: {}",
                chatroomId, consultationType);

        // 관리자 권한 검증
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (adminUser.getUserType() != UserType.ADMIN) {
            throw new GlobalException(ChatErrorCode.UNAUTHORIZED_SYSTEM_MESSAGE);
        }

        // 채팅방 존재 확인
        Chatroom chatroom = chatroomRepository.findById(chatroomId)
                .orElseThrow(() -> new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND));

        // 알림 메시지 생성
        String notificationContent = buildReservationNotification(
                memberName,
                scheduledDateTime,
                consultationType
        );

        // 시스템 메시지 생성 및 저장
        Message systemMessage = Message.builder()
                .chatroomId(chatroomId)
                .senderId(adminUserId) // 관리자 ID
                .content(notificationContent)
                .messageType(MessageType.SYSTEM)
                .relatedId(consultationId)
                .build();

        Message savedMessage = chatMessageRepository.save(systemMessage);

        // 저장된 엔티티에서 DTO로 변환
        ChatMessageDTO messageDTO = savedMessage.toDTO();

        SocketResponseDTO<ChatMessageDTO> response = SocketResponseDTO.message(chatroomId, messageDTO);
        messagingTemplate.convertAndSend("/sub/chatrooms/" + chatroomId, response);

        log.info("상담 예약 알림 전송 완료 - messageId: {}, chatroomId: {}",
                savedMessage.getId(), chatroomId);
    }

    /**
     * 예약 알림 메시지 포맷 생성
     */
    private String buildReservationNotification(
            String memberName,
            LocalDateTime scheduledDateTime,
            ConsultationType consultationType
    ) {
        StringBuilder sb = new StringBuilder();

        // 상담 타입에 따른 제목
        if (consultationType == ConsultationType.MESSAGE) {
            sb.append("[📩 메시지 상담 예약이 접수되었습니다]\n\n");
        } else if (consultationType == ConsultationType.VIDEO) {
            sb.append("[🎥 화상 상담 예약이 접수되었습니다]\n\n");
        }

        sb.append("상담 전 고민지 내용을 확인하고 상담을 시작해 주세요.\n\n");

        // 고객명
        sb.append("▶ 고객명: ").append(memberName).append(" 님\n");

        // 상담 일정
        sb.append("▶ 상담 일정: ");
        if (scheduledDateTime != null) {
            // VIDEO 상담의 경우 일정 있음
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
            sb.append(scheduledDateTime.format(formatter));
        } else {
            // MESSAGE 상담의 경우 일정 없음 (즉시 시작)
            sb.append("즉시 시작");
        }
        sb.append("\n");

        // 상담 종류
        sb.append("▶ 상담 종류: ");
        if (consultationType == ConsultationType.MESSAGE) {
            sb.append("메시지 상담");
        } else if (consultationType == ConsultationType.VIDEO) {
            sb.append("화상 상담");
        }
        sb.append("\n\n");

        // 안내 사항 (상담 타입에 따라 다름)
        sb.append("📌 상담 진행 안내\n");

        if (consultationType == ConsultationType.MESSAGE) {
            // MESSAGE 상담 안내
            sb.append("• 상담 일정에 맞춰 메시지 상담을 진행해 주세요.\n");
            sb.append("• 상담 중 추가로 필요한 정보가 있다면 질문할 수 있어요.\n");
            sb.append("• 상담이 종료된 후, 솔루션지를 작성해 주세요.");

        } else if (consultationType == ConsultationType.VIDEO) {
            // VIDEO 상담 안내
            sb.append("• 상담 시작 10분 전, 줌 링크를 생성해 앱 내 채팅을 통해 고객에게 전달해 주세요.\n");
            sb.append("• 예약된 시간 내에 화상 상담을 진행해 주세요.\n");
            sb.append("• 상담 전 추가로 필요한 정보가 있다면 질문할 수 있어요.\n");
            sb.append("• 상담이 종료된 후, 솔루션지를 작성해 주세요.\n\n");
            sb.append("📋 고민지 확인하기");
        }

        return sb.toString();
    }

}