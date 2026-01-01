package com.ceos.menual.domain.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전문가 프로필 요약 응답 DTO")
public class ExpertSummaryResponseDTO {

    @Schema(description = "전문가 ID")
    private Long expertId;

    @Schema(description = "전문가 닉네임")
    private String nickname;

    @Schema(description = "카테고리")
    private String category;

    @Schema(description = "전문가 프로필 이미지 URL")
    private String profileImage; // User 엔티티에 있는 이미지

    @Schema(description = "한줄 소개")
    private String introduction;

    @Schema(description = "평균 평점")
    private Double ratingAverage; // Double

    @Schema(description = "리뷰 참여자 수")
    private Long reviewCount;

    @Schema(description = "대표 리뷰 이미지 (최대 3개)")
    private List<String> representativeReviewImages;


    public void setImages(List<String> images) {
        this.representativeReviewImages = images;
    }
}