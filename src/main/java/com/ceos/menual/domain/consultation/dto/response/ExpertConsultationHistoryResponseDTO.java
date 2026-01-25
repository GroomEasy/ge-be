package com.ceos.menual.domain.consultation.dto.response;

import com.ceos.menual.entity.enums.Category;
import com.ceos.menual.entity.enums.ConsultationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Builder
public class ExpertConsultationHistoryResponseDTO {

    private Long consultationId;
    private Long reservationId;
    private Long chatroomId;

    // 회원 정보
    private Long memberUserId;
    private String memberNickname;
    private String memberProfileImage;

    // 상담 정보
    private Category category;
    private ConsultationType consultationType;
    private LocalDateTime consultationDate;
    private Integer price;

    // 솔루션 작성 여부
    private Boolean solutionWritten;

    // 후기 작성 여부
    private Boolean reviewWritten;

    /**
     * QueryDSL Constructor용 생성자
     */
    public ExpertConsultationHistoryResponseDTO(
            Long consultationId,
            Long reservationId,
            Long chatroomId,
            Long memberUserId,
            String memberNickname,
            String memberProfileImage,
            Category category,
            ConsultationType consultationType,
            LocalDateTime consultationDate,
            Integer price,
            Boolean solutionWritten,
            Boolean reviewWritten
    ) {
        this.consultationId = consultationId;
        this.reservationId = reservationId;
        this.chatroomId = chatroomId;
        this.memberUserId = memberUserId;
        this.memberNickname = memberNickname;
        this.memberProfileImage = memberProfileImage;
        this.category = category;
        this.consultationType = consultationType;
        this.consultationDate = consultationDate;
        this.price = price;
        this.solutionWritten = solutionWritten != null ? solutionWritten : false;
        this.reviewWritten = reviewWritten != null ? reviewWritten : false;
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
