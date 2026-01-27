package com.ceos.menual.domain.expert.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포트폴리오 수정 요청 DTO (변경할 필드만 전달)")
public class PortfolioUpdateRequestDTO {

	@Schema(description = "시술명/제목(최대 100자)", example = "손상모 복구 레이어드 컷")
	@Size(max = 100, message = "시술명은 100자 이내여야 합니다.")
	private String title;

	@Schema(description = "고객의 고민(최대 500자)", example = "잦은 탈색으로 모발 끝이 갈라지고 부스스함")
	@Size(max = 500, message = "고민은 500자 이내여야 합니다.")
	private String concern;

	@Schema(description = "해결 솔루션(최대 1000자)", example = "단백질 케어와 함께 레이어드 컷으로 손상 부위 제거")
	@Size(max = 1000, message = "솔루션은 1000자 이내여야 합니다.")
	private String solution;

	@Schema(
		description = "시술 전 이미지 S3 tmp key(선택). 전달 시 final로 이동 후 DB에는 final 공개 URL 저장",
		example = "tmp/portfolio/expert-123/before/550e8400-e29b-41d4-a716-446655440000.jpg"
	)
	private String beforeImage;

	@Schema(
		description = "시술 후 이미지 S3 tmp key(선택). 전달 시 final로 이동 후 DB에는 final 공개 URL 저장",
		example = "tmp/portfolio/expert-123/after/550e8400-e29b-41d4-a716-446655440000.jpg"
	)
	private String afterImage;

	@Schema(description = "해시태그 이름 리스트(최대 10개). 전달 시 전체 교체", example = "[\"레이어드컷\", \"복구펌\", \"가을헤어\"]")
	@Size(max = 10, message = "해시태그는 최대 10개까지 가능합니다.")
	private List<@NotBlank(message = "빈 해시태그는 입력할 수 없습니다.") @Size(max = 20, message = "해시태그는 20자를 초과할 수 없습니다.") String> hashtags;

	@AssertTrue(message = "수정할 값이 없습니다. (title/concern/solution/beforeImage/afterImage/hashtags 중 최소 1개 필요)")
	private boolean isAtLeastOneProvided() {
		return (title != null && !title.trim().isEmpty())
			|| (concern != null && !concern.trim().isEmpty())
			|| (solution != null && !solution.trim().isEmpty())
			|| (beforeImage != null && !beforeImage.trim().isEmpty())
			|| (afterImage != null && !afterImage.trim().isEmpty())
			|| hashtags != null;
	}
}

