package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static StyleFit fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (StyleFit fit : StyleFit.values()) {
            if (fit.korean.equals(value)) {
                return fit;
            }
        }
        throw new IllegalArgumentException("Unknown StyleFit: " + value);
    }
}
