package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StyleImage {
    SEXY("섹시함", "Sexy"),
    NEAT("단정함", "Neat"),
    CUTE("귀여움", "Cute"),
    MASCULINE("남자다움", "Masculine"),
    TRENDY("힙한", "Trendy"),
    GLAMOROUS("화려한", "Glamorous"),
    NATURAL("자연스러움", "Natural"),
    OTHER("기타", "Other");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }
}
