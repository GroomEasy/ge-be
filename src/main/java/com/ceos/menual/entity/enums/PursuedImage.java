package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PursuedImage {
    SEXY("섹시함", "Sexy"),
    NEAT("단정함", "Neat"),
    CUTE("귀여움", "Cute"),
    MASCULINE("남자다움", "Masculine"),
    TRUSTWORTHY("신뢰를 주는", "Trustworthy"),
    GLAMOROUS("화려한", "Glamorous"),
    NATURAL("자연스러움", "Natural"),
    OTHER("기타", "Other");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }

    @JsonCreator
    public static PursuedImage fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (PursuedImage image : PursuedImage.values()) {
            if (image.korean.equals(value)) {
                return image;
            }
        }
        throw new IllegalArgumentException("Unknown PursuedImage: " + value);
    }
}
