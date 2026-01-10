package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleColor {
    MONOCHROME("무채색", "Monochrome"),
    COLORFUL("컬러풀", "Colorful");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }
}
