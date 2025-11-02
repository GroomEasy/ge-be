package com.groomeasy.backend.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.groomeasy.backend.entity.enums.UserType;
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
