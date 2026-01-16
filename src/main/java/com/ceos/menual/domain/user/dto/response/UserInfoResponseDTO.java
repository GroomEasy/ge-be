package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.entity.User;
import com.ceos.menual.entity.enums.UserType;
import com.ceos.menual.global.exception.GlobalException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponseDTO {

    private Long userId;
    private UserType userType;
    private String nickname;
    private Long expertLikeCount;
    private Integer points;
    private Long reviewCount;

    public static UserInfoResponseDTO of(User user, Long expertLikeCount, Long reviewCount) {
        if (user == null) {
            throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
        }

        // 전문가(EXPERT)이면 null, 일반 유저이면 프로필에서 포인트 추출
        Integer points = null;
        if (user.getUserType() == UserType.MEMBER && user.getGeneralProfile() != null) {
            points = user.getGeneralProfile().getTotalPoints();
        }

        return UserInfoResponseDTO.builder()
                .userId(user.getId())
                .userType(user.getUserType())
                .nickname(user.getNickname())
                .expertLikeCount(expertLikeCount)
                .points(points)
                .reviewCount(reviewCount)
                .build();
    }
}