package com.ceos.menual.domain.user.dto.request;

import com.ceos.menual.entity.enums.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전문가 전환 요청 DTO")
public class ExpertConversionRequestDTO {

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(description = "전문가 카테고리", example = "HAIR", required = true)
    private Category category;

    @Schema(description = "전문 분야 리스트 (선택)", example = "[\"다운펌\", \"댄디펌\"]")
    private List<String> specialities;

    @Schema(description = "한줄소개 (선택)", example = "10년차 헤어 스타일 전문가입니다.")
    private String introduction;

    @Schema(description = "프로필 링크 (선택)", example = "https://menual.site/")
    private String profileLink;

    @Schema(description = "경력정보 (선택)", example = "준O헤어 근무\n...")
    private String careerInfo;

    // ExpertBankAccount 관련 필드 (선택)
    @Schema(description = "은행명 (선택)", example = "국민은행")
    private String bankName;

    @Schema(description = "계좌번호 (선택)", example = "123456-78-901234")
    private String accountNumber;

    @Schema(description = "예금주명 (선택)", example = "홍길동")
    private String accountHolder;

    // 계좌 정보가 모두 입력되었는지 확인하는 메서드
    public boolean hasCompleteBankAccountInfo() {
        return bankName != null && accountNumber != null && accountHolder != null;
    }

    // 계좌 정보가 일부만 입력되었는지 확인하는 메서드
    public boolean hasPartialBankAccountInfo() {
        int count = 0;
        if (bankName != null) count++;
        if (accountNumber != null) count++;
        if (accountHolder != null) count++;
        return count > 0 && count < 3;
    }
}