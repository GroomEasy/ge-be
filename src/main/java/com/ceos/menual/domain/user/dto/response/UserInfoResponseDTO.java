package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.entity.User;
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

    public static UserInfoResponseDTO from(User user) {
        return UserInfoResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
    }
}
