package com.ceos.menual.domain.expert.service.zoom;

import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.expert.api.ZoomOauthClient;
import com.ceos.menual.domain.expert.dto.response.ZoomCreateMeetingResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ZoomTokenResponseDTO;
import com.ceos.menual.domain.expert.exception.ZoomErrorCode;
import com.ceos.menual.global.exception.GlobalException;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.enums.ConsultationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoomMeetingService {

    private final ZoomProperties zoomProperties;
    private final ZoomOauthClient zoomOauthClient;
    private final WebClient zoomApiWebClient;
    private final ConsultationRepository consultationRepository;

    @Transactional
    public void createMeetingAndSave(Long consultationId) {
        Consultation consultation = consultationRepository.findByIdWithAllRelations(consultationId)
            .orElseThrow(() -> new GlobalException(com.ceos.menual.domain.consultation.exception.ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        log.info("Zoom meeting create requested - consultationId={}, type={}, alreadyCreated={}",
            consultationId, consultation.getType(), consultation.getIsZoomMeetingCreated());

        if (consultation.getType() != ConsultationType.VIDEO) return;
        if (Boolean.TRUE.equals(consultation.getIsZoomMeetingCreated())) return;

        ExpertProfile expertProfile = consultation.getExpertProfile();
        if (expertProfile == null || !Boolean.TRUE.equals(expertProfile.getIsZoomConnected())
            || expertProfile.getZoomUserId() == null
            || expertProfile.getZoomAccessToken() == null
            || expertProfile.getZoomRefreshToken() == null) {
            throw new GlobalException(ZoomErrorCode.ZOOM_NOT_CONNECTED);
        }

        String accessToken = getValidAccessToken(expertProfile);

        ZoomCreateMeetingResponseDTO meeting = callCreateMeeting(
            expertProfile.getZoomUserId(),
            accessToken,
            consultation
        );

        consultation.attachZoomMeeting(meeting.getId(), meeting.getJoinUrl());
        consultationRepository.saveAndFlush(consultation);

        // 저장 확인(디버깅): DB 재조회로 값이 실제로 들어갔는지 로그로 확정
        consultationRepository.findById(consultationId).ifPresent(saved -> {
            log.info("Zoom meeting saved - consultationId={}, zoomMeetingId={}, zoomJoinUrlPresent={}",
                consultationId,
                saved.getZoomMeetingId(),
                saved.getZoomJoinUrl() != null && !saved.getZoomJoinUrl().isBlank()
            );
        });
    }

    /**
     * AFTER_COMMIT 리스너에서 호출되는 외부 API + DB 갱신 로직은 "커밋 이후"라 기존 트랜잭션이 종료된 상태
     * -> 별도의 새 트랜잭션에서 저장/flush가 가능하도록 분리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMeetingAndSaveInNewTx(Long consultationId) {
        createMeetingAndSave(consultationId);
    }

    private String getValidAccessToken(ExpertProfile expertProfile) {
        LocalDateTime expiresAt = expertProfile.getZoomTokenExpiresAt();
        boolean expired = expiresAt != null && expiresAt.isBefore(LocalDateTime.now().minusSeconds(30));
        if (!expired) return expertProfile.getZoomAccessToken();

        ZoomTokenResponseDTO refreshed = zoomOauthClient.refreshAccessToken(
            zoomProperties.getClientId(),
            zoomProperties.getClientSecret(),
            expertProfile.getZoomRefreshToken()
        );

        LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(
            refreshed.getExpiresIn() != null ? refreshed.getExpiresIn() : 3600
        );
        expertProfile.updateZoomAccessToken(refreshed.getAccessToken(), newExpiresAt);

        // Zoom은 refresh 응답에 refresh_token을 같이 줄 수도/안 줄 수도 있음
        if (refreshed.getRefreshToken() != null && !refreshed.getRefreshToken().isBlank()) {
            expertProfile.connectZoom(expertProfile.getZoomUserId(), refreshed.getAccessToken(), refreshed.getRefreshToken(), newExpiresAt);
        }

        return refreshed.getAccessToken();
    }

    private ZoomCreateMeetingResponseDTO callCreateMeeting(String zoomUserId, String accessToken, Consultation consultation) {
        try {
            LocalDateTime start = consultation.getScheduleTime();
            if (start == null) start = LocalDateTime.now().plusMinutes(10);

            int duration = consultation.getDurationMinutes() != null ? consultation.getDurationMinutes() : 60;

            String topic = "[Menual] 화상 상담";
            String startTime = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Map<String, Object> body = Map.of(
                "topic", topic,
                "type", 2,
                "start_time", startTime,
                "duration", duration,
                "timezone", "Asia/Seoul",
                "settings", Map.of(
                    "join_before_host", false,
                    "waiting_room", true
                )
            );

            ZoomCreateMeetingResponseDTO res = zoomApiWebClient.post()
                .uri("/v2/users/{userId}/meetings", zoomUserId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ZoomCreateMeetingResponseDTO.class)
                .block();

            if (res == null || res.getId() == null || res.getJoinUrl() == null) {
                throw new GlobalException(ZoomErrorCode.ZOOM_MEETING_CREATE_FAILED);
            }
            return res;
        } catch (WebClientResponseException e) {
            // Zoom이 4xx/5xx를 내려주는 케이스(권한 부족/토큰 만료/스코프 부족 등)
            log.error("Zoom create meeting failed - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ZoomErrorCode.ZOOM_MEETING_CREATE_FAILED);
        } catch (WebClientRequestException e) {
            throw new GlobalException(ZoomErrorCode.ZOOM_MEETING_CREATE_FAILED);
        }
    }
}

