package com.ceos.menual.domain.reservation.scheduler;

import com.ceos.menual.domain.reservation.repository.ReservationRepository;
import com.ceos.menual.entity.Reservation;
import com.ceos.menual.entity.enums.ReservationStatus;
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
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;

    /**
     * 5분마다 만료된 미입금 예약을 CANCELLED 상태로 변경
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expireUnpaidReservations() {
        log.info("만료된 미입금 예약 체크 시작");

        List<Reservation> expiredReservations =
                reservationRepository.findByReservationStatusAndExpiresAtBefore(
                        ReservationStatus.UNPAID,
                        LocalDateTime.now()
                );

        if (expiredReservations.isEmpty()) {
            log.info("만료된 예약 없음");
            return;
        }

        expiredReservations.forEach(reservation -> {
            reservation.cancel();
            log.info("예약 만료 처리 - reservationId: {}, expertProfileId: {}",
                    reservation.getId(), reservation.getExpertProfile().getId());
        });

        reservationRepository.saveAll(expiredReservations);
        log.info("만료된 예약 {}건 처리 완료", expiredReservations.size());
    }
}


