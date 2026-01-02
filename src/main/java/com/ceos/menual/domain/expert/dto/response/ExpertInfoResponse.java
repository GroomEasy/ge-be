package com.ceos.menual.domain.expert.dto.response;

import com.ceos.menual.entity.ExpertProfile;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertInfoResponse {

    private Long userId;  // 전문가 ID
    private String nickname;
    private String profileImage;
    private Category category;
    private List<String> specialities;
    private String introduction;
    private String profileLink;
    private String careerInfo;

    //TODO: 찜 목록 구현
//    private Integer likes;

    public static ExpertInfoResponse from(User user) {
        ExpertProfile expertProfile = user.getExpertProfile();

        return ExpertInfoResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .category(expertProfile.getCategory())
                .specialities(expertProfile.getSpecialities())
                .introduction(expertProfile.getIntroduction())
                .profileLink(expertProfile.getProfileLink())
                .careerInfo(expertProfile.getCareerInfo())
                .build();
    }
}