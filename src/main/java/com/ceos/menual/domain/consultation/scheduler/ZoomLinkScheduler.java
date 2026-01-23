package com.ceos.menual.domain.consultation.scheduler;

import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.chat.service.ChatMessageService;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.enums.ChatroomType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZoomLinkScheduler {

    private final ConsultationRepository consultationRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatMessageService chatMessageService;

    /**
     * 매 분마다 상담 시간 10분 전인 화상 상담에 Zoom 링크 메시지 자동 전송
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendZoomLinkMessages() {
        log.debug("Zoom 링크 메시지 전송 스케줄러 실행");

        LocalDateTime now = LocalDateTime.now();
        // 현재 시간 + 9분 ~ 현재 시간 + 11분 사이의 상담 조회 (10분 전후 1분 여유)
        LocalDateTime startTime = now.plusMinutes(9);
        LocalDateTime endTime = now.plusMinutes(11);

        List<Consultation> consultations = consultationRepository
                .findVideoConsultationsForZoomLinkNotification(startTime, endTime);

        if (consultations.isEmpty()) {
            log.debug("Zoom 링크 전송 대상 상담 없음");
            return;
        }

        log.info("Zoom 링크 전송 대상 상담 {}건 발견", consultations.size());

        for (Consultation consultation : consultations) {
            try {
                sendZoomLinkForConsultation(consultation);
            } catch (Exception e) {
                log.error("Zoom 링크 메시지 전송 실패 - consultationId: {}", consultation.getId(), e);
            }
        }
    }

    private void sendZoomLinkForConsultation(Consultation consultation) {
        Long consultationId = consultation.getId();
        String zoomJoinUrl = consultation.getZoomJoinUrl();

        if (zoomJoinUrl == null || zoomJoinUrl.isEmpty()) {
            log.warn("Zoom 링크 없음 - consultationId: {}", consultationId);
            return;
        }

        // consultationId로 VIDEO 타입 chatroom 조회
        Chatroom chatroom = chatroomRepository
                .findByConsultationIdAndChatroomType(consultationId, ChatroomType.VIDEO)
                .orElse(null);

        if (chatroom == null) {
            log.warn("채팅방 없음 - consultationId: {}", consultationId);
            return;
        }

        Long expertId = consultation.getExpertProfile().getUser().getId();

        // Zoom 링크 메시지 전송
        chatMessageService.sendZoomLinkMessage(
                chatroom.getId(),
                expertId,
                zoomJoinUrl,
                consultationId
        );

        // 전송 완료 플래그 업데이트
        consultation.markZoomLinkSent();
        consultationRepository.save(consultation);

        log.info("Zoom 링크 메시지 전송 완료 - consultationId: {}, chatroomId: {}, scheduleTime: {}",
                consultationId, chatroom.getId(), consultation.getScheduleTime());
    }

    /**
     * 매 분마다 시작 시간이 된 화상 상담의 상태를 READY → IN_PROGRESS로 변경
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void startVideoConsultations() {
        log.debug("화상 상담 시작 스케줄러 실행");

        LocalDateTime now = LocalDateTime.now();

        List<Consultation> consultations = consultationRepository
                .findVideoConsultationsToStart(now);

        if (consultations.isEmpty()) {
            log.debug("시작 대상 화상 상담 없음");
            return;
        }

        log.info("시작 대상 화상 상담 {}건 발견", consultations.size());

        for (Consultation consultation : consultations) {
            try {
                consultation.startConsultation();
                consultationRepository.save(consultation);
                log.info("화상 상담 시작 처리 완료 - consultationId: {}, videoStartTime: {}",
                        consultation.getId(), consultation.getVideoStartTime());
            } catch (Exception e) {
                log.error("화상 상담 시작 처리 실패 - consultationId: {}", consultation.getId(), e);
            }
        }
    }
}
