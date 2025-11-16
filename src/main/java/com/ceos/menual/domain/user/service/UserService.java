package com.ceos.menual.domain.user.service;

import com.ceos.menual.domain.user.dto.request.SignUpRequestDTO;
import com.ceos.menual.domain.user.dto.response.SignUpResponseDTO;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.AuthProvider;

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
            // TODO: Error Code 추가
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        //TODO: 예외처리
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
            // TODO: Error Code 추가
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            // TODO: Error Code 추가
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
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
            throw new IllegalArgumentException("올바르지 않은 생년월일 형식입니다.");
        }
    }

}
