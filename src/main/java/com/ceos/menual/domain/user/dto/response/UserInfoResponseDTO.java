package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.domain.user.exception.UserErrorCode;
import com.ceos.menual.entity.User;
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
    private String nickname;
    private Long expertLikeCount;
    private Integer points;
    private Long reviewCount;

    public static UserInfoResponseDTO of(User user, Long expertLikeCount, Long reviewCount) {

        if (user == null) {
                throw new GlobalException(UserErrorCode.USER_NOT_FOUND);
        };

        Integer points = user.getGeneralProfile() != null
                ? user.getGeneralProfile().getTotalPoints()
                : 0;

        return UserInfoResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .expertLikeCount(expertLikeCount)
                .points(points)
                .reviewCount(reviewCount)
                .build();
    }
}
