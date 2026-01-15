package com.ceos.menual.domain.expert.service.zoom;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ZoomOAuthCallbackResult {
    private Long expertUserId;
    private String zoomUserId;
    private String zoomEmail;

    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;
}

