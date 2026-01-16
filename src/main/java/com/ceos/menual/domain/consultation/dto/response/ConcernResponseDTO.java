package com.ceos.menual.domain.consultation.dto.response;

import lombok.Builder;

@Builder
public record ConcernResponseDTO(
        String nickname,
        String concernsJson
) {
    public static ConcernResponseDTO from(String nickname, String concernsJson) {
        return ConcernResponseDTO.builder()
                .nickname(nickname)
                .concernsJson(concernsJson)
                .build();
    }
}