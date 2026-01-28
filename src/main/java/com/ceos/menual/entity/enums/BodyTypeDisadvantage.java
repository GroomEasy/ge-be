package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BodyTypeDisadvantage {
    NARROW_SHOULDERS("좁은어깨", "Narrow shoulders"),
    THIN_LEGS("얇은다리", "Thin legs"),
    THIN_ARMS("얇은팔", "Thin arms"),
    PROTRUDING_BELLY("볼록한배", "Protruding belly"),
    THICK_LEGS("굵은다리", "Thick legs"),
    LARGE_TORSO("큰몸통", "Large torso"),
    SLENDER_BODY("얄상한몸", "Slender body"),
    HEIGHT("키", "Height"),
    UPPER_LOWER_BODY_RATIO("상하체비율", "Upper lower body ratio"),
    HEAD_SIZE("머리크기", "Head size"),
    OTHER("기타", "Other");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }

    @JsonCreator
    public static BodyTypeDisadvantage fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (BodyTypeDisadvantage disadvantage : BodyTypeDisadvantage.values()) {
            if (disadvantage.korean.equals(value)) {
                return disadvantage;
            }
        }
        throw new IllegalArgumentException("Unknown BodyTypeDisadvantage: " + value);
    }
}
