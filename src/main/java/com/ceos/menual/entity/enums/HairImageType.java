package com.ceos.menual.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HairImageType {
    HAIRSTYLE("헤어스타일"),
    FRONT("정면"),
    LEFT_SIDE("왼쪽측면"),
    RIGHT_SIDE("오른쪽측면"),
    FAVORITE_STYLE("가장 마음에 드는 사진"),
    STYLING_DIFFICULTY("스타일링 어려움");

    private final String description;
}
