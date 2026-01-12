package com.ceos.menual.domain.consultation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionRequestDTO {
    
    @NotBlank(message = "솔루션 내용은 필수입니다")
    @Size(min = 1, max = 10000, message = "솔루션 내용은 1자 이상 10000자 이하여야 합니다")
    private String solution;
}
