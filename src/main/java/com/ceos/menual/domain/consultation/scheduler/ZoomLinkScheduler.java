package com.ceos.menual.domain.consultation.scheduler;

import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.chat.service.ChatMessageService;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.enums.ChatroomType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ZoomLinkScheduler {

    private final ConsultationRepository consultationRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatMessageService chatMessageService;
    private final TransactionTemplate readOnlyTransactionTemplate;
    private final TransactionTemplate transactionTemplate;

    public ZoomLinkScheduler(
            ConsultationRepository consultationRepository,
            ChatroomRepository chatroomRepository,
            ChatMessageService chatMessageService,
            PlatformTransactionManager transactionManager
    ) {
        this.consultationRepository = consultationRepository;
        this.chatroomRepository = chatroomRepository;
        this.chatMessageService = chatMessageService;

        // Read-only transaction template for collecting payloads
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);

        // Default transaction template for marking as sent
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Zoom 링크 전송에 필요한 정보를 담는 내부 DTO
     */
    private record ZoomLinkSendPayload(
            Long consultationId,
            Long chatroomId,
            Long expertId,
            String zoomJoinUrl,
            LocalDateTime scheduleTime
    ) {}

    /**
     * 매 분마다 상담 시간 10분 전인 화상 상담에 Zoom 링크 메시지 자동 전송
     *
     * 트랜잭션 분리:
     * 1. DB 조회 및 페이로드 수집 (트랜잭션 내)
     * 2. 외부 메시지 전송 (트랜잭션 외부)
     * 3. 전송 완료 플래그 업데이트 (개별 트랜잭션)
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendZoomLinkMessages() {
        log.debug("Zoom 링크 메시지 전송 스케줄러 실행");

        // 1. 트랜잭션 내에서 전송 대상 조회 및 페이로드 수집 (TransactionTemplate 사용)
        List<ZoomLinkSendPayload> payloads = readOnlyTransactionTemplate.execute(
                status -> collectZoomLinkPayloadsInternal()
        );

        if (payloads == null || payloads.isEmpty()) {
            log.debug("Zoom 링크 전송 대상 상담 없음");
            return;
        }

        log.info("Zoom 링크 전송 대상 상담 {}건 발견", payloads.size());

        // 2. 트랜잭션 외부에서 메시지 전송 및 플래그 업데이트
        for (ZoomLinkSendPayload payload : payloads) {
            try {
                // 외부 메시지 전송 (트랜잭션 외부)
                chatMessageService.sendZoomLinkMessage(
                        payload.chatroomId(),
                        payload.expertId(),
                        payload.zoomJoinUrl(),
                        payload.consultationId()
                );

                // 전송 성공 후 플래그 업데이트 (개별 트랜잭션 - TransactionTemplate 사용)
                transactionTemplate.executeWithoutResult(
                        status -> markZoomLinkSentInternal(payload.consultationId())
                );

                log.info("Zoom 링크 메시지 전송 완료 - consultationId: {}, chatroomId: {}, scheduleTime: {}",
                        payload.consultationId(), payload.chatroomId(), payload.scheduleTime());

            } catch (Exception e) {
                log.error("Zoom 링크 메시지 전송 실패 - consultationId: {}", payload.consultationId(), e);
            }
        }
    }

    /**
     * 전송 대상 조회 및 페이로드 수집 (내부 헬퍼 메서드)
     */
    private List<ZoomLinkSendPayload> collectZoomLinkPayloadsInternal() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.plusMinutes(9);
        LocalDateTime endTime = now.plusMinutes(11);

        List<Consultation> consultations = consultationRepository
                .findVideoConsultationsForZoomLinkNotification(startTime, endTime);

        List<ZoomLinkSendPayload> payloads = new ArrayList<>();

        for (Consultation consultation : consultations) {
            Long consultationId = consultation.getId();
            String zoomJoinUrl = consultation.getZoomJoinUrl();

            if (zoomJoinUrl == null || zoomJoinUrl.isEmpty()) {
                log.warn("Zoom 링크 없음 - consultationId: {}", consultationId);
                continue;
            }

            Chatroom chatroom = chatroomRepository
                    .findByConsultationIdAndChatroomType(consultationId, ChatroomType.VIDEO)
                    .orElse(null);

            if (chatroom == null) {
                log.warn("채팅방 없음 - consultationId: {}", consultationId);
                continue;
            }

            Long expertId = consultation.getExpertProfile().getUser().getId();

            payloads.add(new ZoomLinkSendPayload(
                    consultationId,
                    chatroom.getId(),
                    expertId,
                    zoomJoinUrl,
                    consultation.getScheduleTime()
            ));
        }

        return payloads;
    }

    /**
     * 전송 완료 플래그 업데이트 (내부 헬퍼 메서드)
     */
    private void markZoomLinkSentInternal(Long consultationId) {
        consultationRepository.findById(consultationId).ifPresent(consultation -> {
            consultation.markZoomLinkSent();
            consultationRepository.save(consultation);
        });
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
