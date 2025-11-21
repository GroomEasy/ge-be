package com.ceos.menual.domain.user.service;

import com.ceos.menual.domain.user.dto.request.SignUpRequestDTO;
import com.ceos.menual.domain.user.dto.response.SignUpResponseDTO;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.AuthProvider;

import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResponseDTO signUp(SignUpRequestDTO request) {
        // 비밀번호 일치 검증
        if (!request.isPasswordMatch()) {
            throw new GlobalException(UserErrorCode.INVALID_PASSWORD);
        }

        // 이메일 중복 검사
        validateDuplicateEmail(request.getEmail());

        // 닉네임 중복 검사
        validateDuplicateNickname(request.getNickname());

        // User 엔티티 생성
        User user = User.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .birth(parseBirthDate(request.getBirth()))
                .userType(request.getUserType())
                .provider(AuthProvider.LOCAL)
                .agreeTerms(request.getAgreeTerms())
                .agreePrivacy(request.getAgreePrivacy())
                .build();

        // 저장
        User savedUser = userRepository.save(user);

        // 응답 생성
        return SignUpResponseDTO.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .userType(savedUser.getUserType())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // 관련 메서드

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new GlobalException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new GlobalException(UserErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // YYYYMMDD -> LocalDate 변환
    private LocalDate parseBirthDate(String birth) {
        try {
            String year = birth.substring(0, 4);
            String month = birth.substring(4, 6);
            String day = birth.substring(6, 8);
            return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } catch (Exception e) {
            throw new GlobalException(UserErrorCode.INVALID_BIRTH_FORMAT);
        }
    }

}
