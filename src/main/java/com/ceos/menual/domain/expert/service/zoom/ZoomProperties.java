package com.ceos.menual.domain.expert.service.zoom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "zoom")
public class ZoomProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    /**
     * OAuth 콜백 처리 후 프론트로 리다이렉트할 URI
     * 예: https://menual.site/experts/zoom/success
     */
    private String successRedirectUri;

    /**
     * OAuth 콜백 처리 실패 시 프론트로 리다이렉트할 URI
     * 예: https://menual.site/experts/zoom/failure
     */
    private String failureRedirectUri;
}

