package com.ceos.menual.domain.consultation.scheduler;

import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.chat.service.ChatMessageService;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.enums.ChatroomType;
import com.ceos.menual.global.config.redis.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZoomLinkScheduler {

    private final ConsultationRepository consultationRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatMessageService chatMessageService;
    private final DistributedLockService distributedLockService;

    // 분산 락 키
    private static final String ZOOM_LINK_SEND_LOCK = "zoom-link-send";
    private static final String VIDEO_CONSULTATION_START_LOCK = "video-consultation-start";

    // 락 TTL (초) - 스케줄러 실행 간격보다 짧게 설정
    private static final long LOCK_TTL_SECONDS = 50;

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
     * 분산 환경 안전성:
     * - 분산 락을 사용하여 여러 인스턴스 중 하나만 실행
     *
     * 트랜잭션 분리:
     * 1. DB 조회 및 페이로드 수집 (트랜잭션 내)
     * 2. 외부 메시지 전송 (트랜잭션 외부)
     * 3. 전송 완료 플래그 업데이트 (개별 트랜잭션)
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendZoomLinkMessages() {
        // 분산 락 획득 시도
        if (!distributedLockService.tryLock(ZOOM_LINK_SEND_LOCK, LOCK_TTL_SECONDS)) {
            log.debug("Zoom 링크 전송 스케줄러 - 다른 인스턴스가 실행 중, 스킵");
            return;
        }

        try {
            log.debug("Zoom 링크 메시지 전송 스케줄러 실행");

            // 1. 트랜잭션 내에서 전송 대상 조회 및 페이로드 수집
            List<ZoomLinkSendPayload> payloads = collectZoomLinkPayloads();

            if (payloads.isEmpty()) {
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

                    // 전송 성공 후 플래그 업데이트 (개별 트랜잭션)
                    markZoomLinkSent(payload.consultationId());

                    log.info("Zoom 링크 메시지 전송 완료 - consultationId: {}, chatroomId: {}, scheduleTime: {}",
                            payload.consultationId(), payload.chatroomId(), payload.scheduleTime());

                } catch (Exception e) {
                    log.error("Zoom 링크 메시지 전송 실패 - consultationId: {}", payload.consultationId(), e);
                }
            }
        } finally {
            // 락 해제
            distributedLockService.unlock(ZOOM_LINK_SEND_LOCK);
        }
    }

    /**
     * 트랜잭션 내에서 전송 대상 조회 및 페이로드 수집
     */
    @Transactional(readOnly = true)
    public List<ZoomLinkSendPayload> collectZoomLinkPayloads() {
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
     * 전송 완료 플래그 업데이트 (개별 트랜잭션)
     */
    @Transactional
    public void markZoomLinkSent(Long consultationId) {
        consultationRepository.findById(consultationId).ifPresent(consultation -> {
            consultation.markZoomLinkSent();
            consultationRepository.save(consultation);
        });
    }

    /**
     * 매 분마다 시작 시간이 된 화상 상담의 상태를 READY → IN_PROGRESS로 변경
     *
     * 분산 환경 안전성:
     * - 분산 락을 사용하여 여러 인스턴스 중 하나만 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void startVideoConsultations() {
        // 분산 락 획득 시도
        if (!distributedLockService.tryLock(VIDEO_CONSULTATION_START_LOCK, LOCK_TTL_SECONDS)) {
            log.debug("화상 상담 시작 스케줄러 - 다른 인스턴스가 실행 중, 스킵");
            return;
        }

        try {
            log.debug("화상 상담 시작 스케줄러 실행");

            startVideoConsultationsInternal();
        } finally {
            // 락 해제
            distributedLockService.unlock(VIDEO_CONSULTATION_START_LOCK);
        }
    }

    /**
     * 화상 상담 시작 처리 (내부 트랜잭션 메서드)
     */
    @Transactional
    public void startVideoConsultationsInternal() {
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
