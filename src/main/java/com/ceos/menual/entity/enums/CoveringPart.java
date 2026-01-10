package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CoveringPart {
    EYES("눈", "Eyes"),
    EYEBROWS("눈썹", "Eyebrows"),
    NOSE("코", "Nose"),
    MOUTH("입", "Mouth"),
    FACE_SHAPE("얼굴형", "Face shape"),
    IMAGE_HARMONY("이미지조화", "Image harmony"),
    NOT_SURE("모르겠음", "Not sure"),
    OTHER("기타", "Other");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }

    @JsonCreator
    public static CoveringPart fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (CoveringPart part : CoveringPart.values()) {
            if (part.korean.equals(value)) {
                return part;
            }
        }
        throw new IllegalArgumentException("Unknown CoveringPart: " + value);
    }
}
