package com.ceos.menual.domain.email.service;

import com.ceos.menual.domain.email.dto.request.EmailVerificationConfirmDTO;
import com.ceos.menual.domain.email.dto.request.EmailVerificationRequestDTO;
import com.ceos.menual.domain.email.dto.response.EmailVerificationResponseDTO;
import com.ceos.menual.domain.email.exception.EmailErrorCode;
import com.ceos.menual.domain.email.repository.EmailVerificationStore;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationStore verificationStore;
    private final EmailSender emailSender;
    private final UserRepository userRepository;

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final int CODE_LENGTH = 6;

    /**
     * 인증번호 발송
     */
    public EmailVerificationResponseDTO sendVerificationCode(EmailVerificationRequestDTO request) {
        String email = request.getEmail();

        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new GlobalException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // 이미 발송된 인증번호가 있는지 확인 (재발송 방지)
        String existingCode = verificationStore.getCode(email);
        if (existingCode != null) {
            throw new GlobalException(EmailErrorCode.VERIFICATION_CODE_ALREADY_SENT);
        }

        // 인증번호 생성
        String code = generateVerificationCode();

        // 인증번호 저장 (5분 유효)
        verificationStore.saveCode(email, code, CODE_TTL);

        // 이메일 전송
        sendVerificationEmail(email, code);

        log.info("Verification code sent. email={}", maskEmail(email));

        return EmailVerificationResponseDTO.codeSent(email);
    }

    /**
     * 인증번호 확인
     */
    public EmailVerificationResponseDTO verifyCode(EmailVerificationConfirmDTO request) {
        String email = request.getEmail();
        String inputCode = request.getCode();

        // 저장된 인증번호 조회
        String storedCode = verificationStore.getCode(email);

        if (storedCode == null) {
            throw new GlobalException(EmailErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        // 인증번호 일치 여부 확인
        if (!storedCode.equals(inputCode)) {
            throw new GlobalException(EmailErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 완료 상태 저장 (30분 유효)
        verificationStore.saveVerified(email, VERIFIED_TTL);

        // 인증번호 삭제
        verificationStore.deleteCode(email);

        log.info("Email verified successfully. email={}", maskEmail(email));

        return EmailVerificationResponseDTO.verified(email);
    }

    /**
     * 이메일 인증 여부 확인
     */
    public boolean isEmailVerified(String email) {
        return verificationStore.isVerified(email);
    }

    /**
     * 인증 완료 상태 삭제 (회원가입 완료 후 호출)
     */
    public void clearVerifiedStatus(String email) {
        verificationStore.deleteVerified(email);
    }

    /**
     * 6자리 인증번호 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    /**
     * 인증 이메일 전송
     */
    private void sendVerificationEmail(String email, String code) {
        String subject = "[Menual] 이메일 인증번호 안내";
        String body = buildEmailBody(code);

        emailSender.sendEmail(email, subject, body);
    }

    /**
     * 이메일 마스킹 (PII 보호)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int visibleChars = Math.min(2, localPart.length());
        return localPart.substring(0, visibleChars) + "***" + domain;
    }

    /**
     * 이메일 본문 생성
     */
    private String buildEmailBody(String code) {
        return """
                안녕하세요.
                Menual 회원가입을 위한 인증번호입니다.

                인증번호: %s

                인증번호는 5분간 유효합니다.
        
                감사합니다.
                Menual
                """.formatted(code);
    }
}