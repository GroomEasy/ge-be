package com.ceos.menual.domain.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ZoomCreateMeetingResponseDTO {
    private Long id;

    @JsonProperty("join_url")
    private String joinUrl;
}

