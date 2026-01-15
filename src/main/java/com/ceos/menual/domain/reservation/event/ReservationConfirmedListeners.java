package com.ceos.menual.domain.reservation.event;

import com.ceos.menual.domain.expert.service.zoom.ZoomMeetingService;
import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.domain.reservation.service.ReservationService;
import com.ceos.menual.entity.Reservation;
import com.ceos.menual.global.exception.GlobalException;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConfirmedListeners {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final ZoomMeetingService zoomMeetingService;

    /**
     * 채팅방 생성/고민지 전송
     * - 결제확인 트랜잭션 안에서 동기 실행
     */
    @EventListener
    public void onReservationConfirmedEssential(ReservationConfirmedEvent event) {
        log.info("ReservationConfirmedEvent received (essential) - reservationId={}, consultationId={}, adminUserId={}",
            event.reservationId(), event.consultationId(), event.adminUserId());

        Reservation reservation = reservationRepository.findById(event.reservationId())
            .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getConsultation() == null) {
            throw new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        reservationService.createChatroomsAndSendConcern(
            reservation.getConsultation(),
            reservation,
            event.adminUserId()
        );
    }

    /**
     * 부가 후처리: Zoom 미팅 생성 및 링크 전송
     * - 트랜잭션 커밋 이후 실행(외부 API 호출이 DB 트랜잭션을 길게 잡지 않도록)
     * - 추후 @Async 추가하면 응답시간 최적화 가능
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmedCreateZoom(ReservationConfirmedEvent event) {
        log.info("ReservationConfirmedEvent received (after_commit) - reservationId={}, consultationId={}, adminUserId={}",
            event.reservationId(), event.consultationId(), event.adminUserId());

        // AFTER_COMMIT 리스너는 트랜잭션이 없는 상태일 수 있어 LAZY 접근을 피합니다.
        try {
            // Zoom 미팅은 결제 확정 직후 생성 후 DB에 저장합니다.
            zoomMeetingService.createMeetingAndSaveInNewTx(event.consultationId());
            log.info("Zoom meeting job completed - consultationId={}", event.consultationId());
        } catch (Exception e) {
            // 결제확정 자체는 완료된 상태이므로, 여기서는 로깅만 하고 종료합니다.
            log.error("Zoom meeting job failed - consultationId={}", event.consultationId(), e);
        }
    }
}

