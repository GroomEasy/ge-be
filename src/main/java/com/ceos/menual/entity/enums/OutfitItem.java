package com.ceos.menual.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OutfitItem {
    OUTER("아우터", "Outer"),
    TOP("상의", "Top"),
    BOTTOM("하의", "Bottom"),
    SHOES("신발", "Shoes"),
    ACCESSORY("악세사리", "Accessory"),
    BAG("가방", "Bag");

    private final String korean;
    private final String english;

    @JsonValue
    public String getKorean() {
        return korean;
    }

    @JsonCreator
    public static OutfitItem fromKorean(String value) {
        if (value == null) {
            return null;
        }
        for (OutfitItem item : OutfitItem.values()) {
            if (item.korean.equals(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown OutfitItem: " + value);
    }
}
