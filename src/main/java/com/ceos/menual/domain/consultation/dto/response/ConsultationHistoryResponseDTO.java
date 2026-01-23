package com.ceos.menual.domain.consultation.dto.response;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Builder
public class ConsultationHistoryResponseDTO {

    private Long consultationId;
    private Long reservationId;

    // 전문가 정보
    private Long expertUserId;
    private String expertNickname;
    private String expertProfileImage;
    private Category category;

    // 상담 정보
    private ConsultationType consultationType;
    private LocalDateTime consultationDate; // 상담일
    private Integer price;

    // 전문가 찜 정보
    private Long expertLikeCount; // 전문가 프로필에 대한 찜 개수

    // 후기 정보
    private Boolean canWriteReview; // 후기 작성 가능 여부
    private Boolean reviewWritten; // 후기 작성 완료 여부

    /**
     * QueryDSL Constructor용 생성자
     */
    public ConsultationHistoryResponseDTO(
            Long consultationId,
            Long reservationId,
            Long expertUserId,
            String expertNickname,
            String expertProfileImage,
            Category category,
            ConsultationType consultationType,
            LocalDateTime consultationDate,
            Integer price,
            Long expertLikeCount,
            Boolean canWriteReview,
            Boolean reviewWritten
    ) {
        this.consultationId = consultationId;
        this.reservationId = reservationId;
        this.expertUserId = expertUserId;
        this.expertNickname = expertNickname;
        this.expertProfileImage = expertProfileImage;
        this.category = category;
        this.consultationType = consultationType;
        this.consultationDate = consultationDate;
        this.price = price;
        this.expertLikeCount = expertLikeCount != null ? expertLikeCount : 0L;
        this.canWriteReview = canWriteReview;
        this.reviewWritten = reviewWritten;
    }

    /**
     * 요일을 한글로 반환
     */
    public String getDayOfWeek() {
        if (consultationDate == null) {
            return "";
        }

        DayOfWeek dayOfWeek = consultationDate.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY: return "월요일";
            case TUESDAY: return "화요일";
            case WEDNESDAY: return "수요일";
            case THURSDAY: return "목요일";
            case FRIDAY: return "금요일";
            case SATURDAY: return "토요일";
            case SUNDAY: return "일요일";
            default: return "";
        }
    }
}