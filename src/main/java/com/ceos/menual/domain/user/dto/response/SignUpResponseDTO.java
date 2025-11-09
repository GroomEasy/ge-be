package com.ceos.menual.domain.user.dto.response;

import com.ceos.menual.entity.enums.UserType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SignUpResponseDTO {

    private Long userId;
    private String email;
    private String nickname;
    private UserType userType;

    private LocalDateTime createdAt;


}
