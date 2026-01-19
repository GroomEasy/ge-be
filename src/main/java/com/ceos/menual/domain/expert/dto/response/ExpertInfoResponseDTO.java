package com.ceos.menual.domain.expert.dto.response;

import com.ceos.menual.domain.expert.exception.ExpertErrorCode;
import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.global.exception.GlobalException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertInfoResponseDTO {

    private Long userId;  // 전문가 ID
    private String nickname;
    private String profileImage;
    private String backgroundImage;
    private Category category;
    private List<String> specialities;
    private String introduction;
    private String profileLink;
    private String careerInfo;

    private Integer likes;

    public static ExpertInfoResponseDTO from(User user, Integer likes) {
        ExpertProfile expertProfile = user.getExpertProfile();

        if (expertProfile == null) {
            throw new GlobalException(ExpertErrorCode.EXPERT_PROFILE_NOT_FOUND);
        }

        return ExpertInfoResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .backgroundImage(expertProfile.getBackgroundImage())
                .category(expertProfile.getCategory())
                .specialities(expertProfile.getSpecialities())
                .introduction(expertProfile.getIntroduction())
                .profileLink(expertProfile.getProfileLink())
                .careerInfo(expertProfile.getCareerInfo())
                .likes(likes)
                .build();
    }
}