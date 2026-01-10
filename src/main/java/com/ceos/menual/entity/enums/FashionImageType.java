package com.ceos.menual.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FashionImageType {
    FRONT_FULL_BODY("정면전신"),
    LEFT_FULL_BODY("왼쪽전신"),
    RIGHT_FULL_BODY("오른쪽전신"),
    FAVORITE_OUTFIT("가장 좋아하는 사진"),
    CONSULTATION_PURPOSE("전문가 상담 목적");

    private final String description;
}
