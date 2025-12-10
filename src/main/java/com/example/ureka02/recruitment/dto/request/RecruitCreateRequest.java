package com.example.ureka02.recruitment.dto.request;

import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 모집글 생성 요청
public class RecruitCreateRequest {
    @NotNull(message = "제목은 필수입니다.")
    private String title;

    private String description;

    @Min(value = 2, message = "최소 2명 이상부터 모집 가능합니다.")
    private int totalSpots;

    @NotNull(message = "마감 시간은 필수입니다.")
    @Future(message = "마감시간은 현재 시간 이후여야 합니다.") // 미래 시간인지 검증
    private LocalDateTime endTime;

}
