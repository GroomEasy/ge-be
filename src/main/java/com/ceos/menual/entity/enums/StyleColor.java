package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static StyleColor fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (StyleColor color : StyleColor.values()) {
            if (color.korean.equals(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown StyleColor: " + value);
    }
}
