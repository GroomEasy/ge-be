package com.ceos.menual.domain.expert.service;

import com.ceos.menual.domain.expert.dto.response.ExpertInfoResponseDTO;
import com.ceos.menual.domain.expert.dto.response.ExpertSummaryResponseDTO;
import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.domain.expert.repository.ExpertLikeRepository;
import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.domain.user.repository.UserRepository;
import com.ceos.menual.entity.ExpertLike;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.GeneralProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertLikeService {

    private final UserRepository userRepository;
    private final ExpertLikeRepository expertLikeRepository;

    /**
     * 전문가 찜하기
     */
    @Transactional
    public void likeExpert(Long currentUserId, Long expertUserId) {
        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        GeneralProfile generalProfile = currentUser.getGeneralProfile();
        if (generalProfile == null) {
            throw new GlobalException(UserErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        // 전문가 사용자 조회
        User expertUser = userRepository.findById(expertUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (!expertUser.isExpert()) {
            throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
        }

        ExpertProfile expertProfile = expertUser.getExpertProfile();

        // 자기 자신 찜 방지
        if (currentUserId.equals(expertUserId)) {
            throw new GlobalException(ExpertErrorCode.CANNOT_LIKE_SELF);
        }

        // 중복 찜 방지
        if (expertLikeRepository.existsByGeneralProfileAndExpertProfile(generalProfile, expertProfile)) {
            throw new GlobalException(ExpertErrorCode.ALREADY_LIKED_EXPERT);
        }

        // 찜 생성
        ExpertLike expertLike = ExpertLike.builder()
                .generalProfile(generalProfile)
                .expertProfile(expertProfile)
                .build();

        expertLikeRepository.save(expertLike);
    }

    /**
     * 전문가 찜 취소
     */
    @Transactional
    public void unlikeExpert(Long currentUserId, Long expertUserId) {
        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        GeneralProfile generalProfile = currentUser.getGeneralProfile();
        if (generalProfile == null) {
            throw new GlobalException(UserErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        // 전문가 사용자 조회
        User expertUser = userRepository.findById(expertUserId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        if (!expertUser.isExpert()) {
            throw new GlobalException(ExpertErrorCode.USER_NOT_EXPERT);
        }

        ExpertProfile expertProfile = expertUser.getExpertProfile();

        // 찜 정보 조회
        ExpertLike expertLike = expertLikeRepository
                .findByGeneralProfileAndExpertProfile(generalProfile, expertProfile)
                .orElseThrow(() -> new GlobalException(ExpertErrorCode.LIKE_NOT_FOUND));

        expertLikeRepository.delete(expertLike);
    }

    /**
     * 내가 찜한 전문가 목록 조회
     */
    public List<ExpertSummaryResponseDTO> getLikedExperts(Long userId, Category category, int page, int size) {
        // 1. 일반 회원 프로필 ID 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        GeneralProfile generalProfile = user.getGeneralProfile();
        if (generalProfile == null) {
            throw new GlobalException(UserErrorCode.GENERAL_PROFILE_NOT_FOUND);
        }

        return expertLikeRepository.findLikedExpertList(generalProfile.getId(), category, page, size);
    }
}
