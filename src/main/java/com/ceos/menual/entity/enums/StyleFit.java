package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleFit {
    MUSCLE_FIT("머슬핏", "Muscle fit"),
    REGULAR("레귤러", "Regular"),
    OVERFIT("오버핏", "Overfit");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }
}
