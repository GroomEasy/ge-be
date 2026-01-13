package com.ceos.menual.domain.consultation.service;

import com.ceos.menual.domain.chat.exception.ChatErrorCode;
import com.ceos.menual.domain.chat.repository.ChatroomRepository;
import com.ceos.menual.domain.chat.service.ChatMessageService;
import com.ceos.menual.domain.consultation.dto.request.SolutionRequestDTO;
import com.ceos.menual.domain.consultation.dto.response.ConsultationHistoryResponseDTO;
import com.ceos.menual.domain.common.service.S3PresignedUrlService;
import com.ceos.menual.domain.reservation.exception.ReservationErrorCode;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.Chatroom;
import com.ceos.menual.entity.Consultation;
import com.ceos.menual.domain.consultation.repository.ConsultationRepository;
import com.ceos.menual.domain.consultation.exception.ConsultationErrorCode;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {

    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final ChatroomRepository chatroomRepository;
    private final ChatMessageService chatMessageService;

    /**
     * 지난 상담 내역 조회 (전체)
     * - PAID + 과거 일정만 조회
     * - 카테고리별 필터링 가능
     */
    public List<ConsultationHistoryResponseDTO> getConsultationHistory(Long userId, Category category) {
        log.info("지난 상담 내역 조회 시작 - userId: {}, category: {}", userId, category);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        // GeneralProfile 조회 (일반 회원만 조회 가능)
        if (user.getGeneralProfile() == null) {
            throw new GlobalException(ReservationErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        GeneralProfile generalProfile = user.getGeneralProfile();

        // 지난 상담 내역 전체 조회 (QueryDSL)
        List<ConsultationHistoryResponseDTO> consultations =
                consultationRepository.findConsultationHistory(generalProfile.getId(), category);

        log.info("지난 상담 내역 조회 완료 - 조회된 개수: {}", consultations.size());

        return consultations;
    }
    /**
     * 솔루션 저장 - 해당 전문가만 가능
     * 
     * null-safe: ExpertProfile 없을 시 명확한 에러코드 반환
     * 권한 검증: 해당 전문가만 솔루션 저장 가능
     * S3 이미지 경로 변환: tmp 상대 경로 → final 공개 URL로 변환
     * Dirty checking: @Transactional에서 자동 저장 (명시적 save 불필요)
     */
    @Transactional
    public Consultation saveSolution(Long consultationId, SolutionRequestDTO solutionRequestDTO, Long expertProfileId) {
        log.info("솔루션 저장 시작 - consultationId: {}, expertProfileId: {}", consultationId, expertProfileId);

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // ExpertProfile null-check (NPE 방지)
        if (consultation.getExpertProfile() == null) {
            log.error("상담과 연결된 전문가 프로필이 없습니다 (데이터 무결성 오류) - consultationId: {}", consultationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_EXPERT_PROFILE_NOT_FOUND);
        }

        // 해당 전문가만 솔루션을 저장할 수 있음 (권한 검증)
        if (!consultation.getExpertProfile().getId().equals(expertProfileId)) {
            log.warn("권한 없음 - 요청한 전문가: {}, 상담의 전문가: {}", 
                expertProfileId, consultation.getExpertProfile().getId());
            throw new GlobalException(ConsultationErrorCode.UNAUTHORIZED_CONSULTATION);
        }

        // 솔루션 텍스트에서 임시 이미지 경로를 최종 공개 URL로 변환
        String solutionText = solutionRequestDTO.getSolution();
        if (solutionText != null && !solutionText.trim().isEmpty()) {
            solutionText = convertTempImagePathsToFinalUrls(solutionText, consultationId);
        }

        // 솔루션 업데이트
        consultation.updateSolution(solutionText);
        
        // Dirty checking: @Transactional에서 자동 저장 (명시적 save 불필요)
        log.info("솔루션 저장 완료 - consultationId: {}, solutionLength: {}", 
            consultationId, solutionText != null ? solutionText.length() : 0);

        // 회원에게 솔루션지 알림 전송
        sendSolutionNotificationToMember(consultation);

        return consultation;
    }

    /**
     * 솔루션 조회 - 해당 전문가 또는 상담 회원만 가능
     */
    @Transactional(readOnly = true)
    public String getSolution(Long consultationId, Long userId, String userType) {
        Consultation consultation = consultationRepository.findByIdWithProfiles(consultationId)
                .orElseThrow(() -> new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND));

        // Null 체크
        if (consultation.getExpertProfile() == null || consultation.getExpertProfile().getUser() == null) {
            log.error("상담 ID: {}의 전문가 프로필 또는 사용자가 없습니다", consultationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND);
        }

        if (consultation.getGeneralProfile() == null || consultation.getGeneralProfile().getUser() == null) {
            log.error("상담 ID: {}의 회원 프로필 또는 사용자가 없습니다", consultationId);
            throw new GlobalException(ConsultationErrorCode.CONSULTATION_NOT_FOUND);
        }

        Long expertUserId = consultation.getExpertProfile().getUser().getId();
        Long memberUserId = consultation.getGeneralProfile().getUser().getId();

        log.debug("권한 검증 - 요청 사용자 ID: {}, 타입: {}", userId, userType);
        log.debug("전문가 User ID: {}, 회원 User ID: {}", expertUserId, memberUserId);

        // 해당 전문가 또는 상담 회원만 조회 가능
        boolean isExpert = "EXPERT".equals(userType) && expertUserId.equals(userId);
        boolean isMember = "MEMBER".equals(userType) && memberUserId.equals(userId);

        log.debug("isExpert: {}, isMember: {}", isExpert, isMember);

        if (!isExpert && !isMember) {
            log.warn("권한 없음 - 사용자: {}, 타입: {}", userId, userType);
            throw new GlobalException(ConsultationErrorCode.UNAUTHORIZED_CONSULTATION);
        }

        return consultation.getSolution();
    }

    /**
     * 솔루션 텍스트에서 tmp 이미지 경로를 final 공개 URL로 변환
     * 
     * HTML 이미지 태그의 src 속성에 있는 경로를 찾아서 변환
     * tmp/consultation/reservation-{reservationId}/solution/{fileName} 
     *     → https://bucket.s3.region.amazonaws.com/final/consultation/{consultationId}/solution/{fileName}
     * 
     * 프로세스:
     * 1. HTML에서 tmp 경로 추출
     * 2. S3에서 최종 위치로 파일 이동
     * 3. 공개 S3 URL 생성
     * 4. HTML에서 경로를 URL로 변환
     * 
     * @param solutionText HTML 형식의 솔루션 텍스트
     * @param consultationId 상담 ID
     * @return 이미지 경로가 공개 S3 URL로 변환된 솔루션 텍스트
     */
    private String convertTempImagePathsToFinalUrls(String solutionText, Long consultationId) {
        // HTML img 태그의 src 속성에서 경로 추출
        // 패턴: src="...tmp/consultation/reservation-{resourceId}/solution/{fileName}"
        Pattern pattern = Pattern.compile("src=[\"']([^\"']*tmp/consultation/[^\"']+/solution/[^\"']*)[\"']");
        Matcher matcher = pattern.matcher(solutionText);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String tempImagePath = matcher.group(1);
            log.debug("임시 이미지 경로 발견 - tempImagePath: {}", tempImagePath);
            
            try {
                // 이미지 파일명 추출 (마지막 / 이후의 부분)
                String[] pathParts = tempImagePath.split("/");
                String fileName = pathParts[pathParts.length - 1];
                
                // 최종 S3 경로 생성
                String finalS3Key = String.format("final/consultation/%d/solution/%s", consultationId, fileName);
                
                // S3에서 임시 경로의 파일을 최종 경로로 이동
                s3PresignedUrlService.moveImageFromTempToFinal(tempImagePath, finalS3Key);
                log.info("이미지 이동 완료 - consultationId: {}, from: {}, to: {}", 
                    consultationId, tempImagePath, finalS3Key);
                
                // 공개 S3 URL 생성
                String s3Url = s3PresignedUrlService.generateS3Url(finalS3Key);
                
                // 텍스트에서 경로를 URL로 변환 (특수문자 이스케이프 필수)
                matcher.appendReplacement(result, Matcher.quoteReplacement("src=\"" + s3Url + "\""));
            } catch (Exception e) {
                log.error("이미지 경로 변환 중 오류 - consultationId: {}, tempImagePath: {}", 
                    consultationId, tempImagePath, e);
                // 오류가 발생해도 텍스트는 유지 (임시 경로로)
                matcher.appendReplacement(result, Matcher.quoteReplacement("src=\"" + tempImagePath + "\""));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 회원에게 솔루션지 도착 알림 전송
     */
    private void sendSolutionNotificationToMember(Consultation consultation) {
        Long consultationId = consultation.getId();

        try {
            // consultationId로 활성 채팅방 조회 (MESSAGE 또는 VIDEO)
            Chatroom chatroom = chatroomRepository.findActiveConsultationChatroom(consultationId)
                    .orElseThrow(() -> {
                        log.error("상담 채팅방을 찾을 수 없습니다 - consultationId: {}", consultationId);
                        return new GlobalException(ChatErrorCode.CHATROOM_NOT_FOUND);
                    });

            // 전문가 ID 추출
            Long expertId = consultation.getExpertProfile().getUser().getId();

            // 회원 닉네임 추출
            String memberNickname = consultation.getGeneralProfile().getUser().getNickname();

            // 솔루션지 메시지 전송
            chatMessageService.sendSolutionMessage(
                    chatroom.getId(),
                    expertId,
                    memberNickname,
                    consultationId
            );

            log.info("솔루션지 알림 전송 완료 - consultationId: {}, chatroomId: {}",
                    consultationId, chatroom.getId());

        } catch (GlobalException e) {
            // GlobalException은 그대로 throw (트랜잭션 롤백)
            log.error("솔루션지 알림 전송 실패 - consultationId: {}", consultationId, e);
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외도 로깅 후 throw (트랜잭션 롤백)
            log.error("솔루션지 알림 전송 중 예상치 못한 오류 - consultationId: {}", consultationId, e);
            throw new GlobalException(ChatErrorCode.ERROR_SAVING_MESSAGE);
        }
    }

}
