package com.ceos.menual.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    HAIR("헤어"),
    FASHION("패션"),
    SKIN("피부"),
    MAKEUP("메이크업");

    private final String description; // 한글 설명 저장
}