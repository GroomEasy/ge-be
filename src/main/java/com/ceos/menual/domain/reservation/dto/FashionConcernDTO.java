package com.ceos.menual.domain.reservation.dto;

import com.ceos.menual.entity.enums.BodyTypeDisadvantage;
import com.ceos.menual.entity.enums.OutfitItem;
import com.ceos.menual.entity.enums.StyleColor;
import com.ceos.menual.entity.enums.StyleFit;
import com.ceos.menual.entity.enums.StyleImage;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "패션 상담 고민지")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FashionConcernDTO {

    @Schema(description = "키 (cm)", example = "165", required = true)
    @NotNull(message = "키는 필수입니다")
    private Integer height;

    @Schema(description = "몸무게 (kg)", example = "55", required = true)
    @NotNull(message = "몸무게는 필수입니다")
    private Integer weight;

    @Schema(description = "상의 사이즈", example = "M", allowableValues = {"S", "M", "L", "XL", "XXL"}, required = true)
    @NotNull(message = "상의 사이즈는 필수입니다")
    @JsonProperty("topSize")
    private String topSize;

    @Schema(description = "하의 사이즈", example = "M", allowableValues = {"S", "M", "L", "XL", "XXL"}, required = true)
    @NotNull(message = "하의 사이즈는 필수입니다")
    @JsonProperty("bottomSize")
    private String bottomSize;

    @Schema(description = "체형적 결점 선택지", example = "[\"좁은어깨\", \"볼록한배\"]", required = true)
    @NotEmpty(message = "체형적 결점은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("bodyTypeDisadvantages")
    private List<BodyTypeDisadvantage> bodyTypeDisadvantages;

    @Schema(description = "체형적 결점 기타 텍스트", example = "상체가 발달했어요")
    @JsonProperty("bodyTypeEtcText")
    private String bodyTypeEtcText;

    @Schema(description = "선호하는 색상", example = "[\"무채색\"]", required = true)
    @NotEmpty(message = "선호하는 색상은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("styleColors")
    private List<StyleColor> styleColors;

    @Schema(description = "선호하는 핏감", example = "[\"레귤러\"]", required = true)
    @NotEmpty(message = "선호하는 핏감은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("styleFits")
    private List<StyleFit> styleFits;

    @Schema(description = "선호하는 이미지", example = "[\"단정함\", \"자연스러움\"]", required = true)
    @NotEmpty(message = "선호하는 이미지는 최소 1개 이상 선택해야 합니다")
    @JsonProperty("styleImages")
    private List<StyleImage> styleImages;

    @Schema(description = "선호 스타일 기타 텍스트", example = "클래식한 느낌")
    @JsonProperty("styleEtcText")
    private String styleEtcText;

    @Schema(description = "원하는 착장 아이템 구성", example = "[\"아우터\", \"상의\", \"하의\", \"신발\"]", required = true)
    @NotEmpty(message = "원하는 착장 아이템은 최소 1개 이상 선택해야 합니다")
    @JsonProperty("outfitItems")
    private List<OutfitItem> outfitItems;

    @Schema(description = "선호 가격대", required = true)
    @NotNull(message = "선호 가격대는 필수입니다")
    @Valid
    @JsonProperty("outfitPriceRange")
    private OutfitPriceRangeDTO outfitPriceRange;

    @Schema(description = "원하는 착장 기타 텍스트", example = "일상복")
    @JsonProperty("outfitEtcText")
    private String outfitEtcText;

    @Schema(description = "전문가 상담 목적", example = "직장복 코디 팁", required = true)
    @NotNull(message = "상담 목적은 필수입니다")
    @JsonProperty("consultationPurpose")
    private String consultationPurpose;
}
