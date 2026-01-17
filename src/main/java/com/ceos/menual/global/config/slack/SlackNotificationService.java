package com.ceos.menual.global.config.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async // 비동기 처리: 이 작업이 오래 걸려도 사용자 응답은 바로 나감
    public void sendRefundNotification(Long reservationId, String username, Integer price, String reason) {
        try {
            // Slack 메시지 포맷 (JSON)
            Map<String, Object> request = new HashMap<>();
            request.put("text", "🚨 *환불 요청이 접수되었습니다!*"); // 알림 미리보기 텍스트

            // 상세 내용은 Attachments 또는 Block Kit 사용 (여기선 간단히 text + attachments)
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#FF0000"); // 빨간색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("신청자", username),
                    createField("환불 금액", price + "원"),
                    createField("사유", reason)
            });

            request.put("attachments", new Object[]{attachment});

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 보내기
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 알림 전송 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            // 알림 실패가 전체 로직을 롤백시키면 안 되므로 로그만 찍고 넘어감
        }
    }

    @Async
    public void sendFashionConcernUpdateNotification(Long reservationId, String username, String category) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "👔 *패션 상담 고민지가 업데이트되었습니다!*");

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#36a64f"); // 녹색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("사용자", username),
                    createField("카테고리", category)
            });

            request.put("attachments", new Object[]{attachment});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 패션 고민지 업데이트 알림 전송 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("Slack 패션 고민지 업데이트 알림 전송 실패", e);
        }
    }

    @Async
    public void sendHairConcernUpdateNotification(Long reservationId, String username, String category) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "💇 *헤어 상담 고민지가 업데이트되었습니다!*");

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#36a64f"); // 녹색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("사용자", username),
                    createField("카테고리", category)
            });

            request.put("attachments", new Object[]{attachment});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 헤어 고민지 업데이트 알림 전송 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("Slack 헤어 고민지 업데이트 알림 전송 실패", e);
        }
    }

    @Async
    public void sendPaymentConfirmationNotification(Long reservationId, Long consultationId, String memberName, String expertName, Integer price, String consultationType) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "💰 *결제가 승인되었습니다!*");

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#0066ff"); // 파란색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("상담 ID", String.valueOf(consultationId)),
                    createField("회원", memberName),
                    createField("전문가", expertName),
                    createField("결제 금액", price + "원"),
                    createField("상담 타입", consultationType)
            });

            request.put("attachments", new Object[]{attachment});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 결제 승인 알림 전송 완료 - reservationId: {}, consultationId: {}", reservationId, consultationId);

        } catch (Exception e) {
            log.error("Slack 결제 승인 알림 전송 실패", e);
        }
    }

    @Async
    public void sendRefundRequestNotification(Long reservationId, String username, Integer price) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "⚠️ *환불 요청이 접수되었습니다!*");

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#ff9900"); // 주황색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("요청자", username),
                    createField("환불 금액", price + "원")
            });

            request.put("attachments", new Object[]{attachment});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 환불 요청 알림 전송 완료 - reservationId: {}", reservationId);

        } catch (Exception e) {
            log.error("Slack 환불 요청 알림 전송 실패", e);
        }
    }

    @Async
    public void sendRefundApprovalNotification(Long reservationId, Long consultationId, String memberName, Integer price) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", "✅ *환불이 승인되었습니다!*");

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "#00cc00"); // 밝은 녹색 띠
            attachment.put("fields", new Object[]{
                    createField("예약 ID", String.valueOf(reservationId)),
                    createField("상담 ID", String.valueOf(consultationId)),
                    createField("회원", memberName),
                    createField("환불 금액", price + "원")
            });

            request.put("attachments", new Object[]{attachment});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(slackWebhookUrl, entity, String.class);

            log.info("Slack 환불 승인 알림 전송 완료 - reservationId: {}, consultationId: {}", reservationId, consultationId);

        } catch (Exception e) {
            log.error("Slack 환불 승인 알림 전송 실패", e);
        }
    }

    private Map<String, Object> createField(String title, String value) {
        Map<String, Object> field = new HashMap<>();
        field.put("title", title);
        field.put("value", value);
        field.put("short", true); // 한 줄에 두 개씩 표시할지 여부
        return field;
    }
}