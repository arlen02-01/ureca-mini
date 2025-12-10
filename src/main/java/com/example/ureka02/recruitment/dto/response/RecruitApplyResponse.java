package com.example.ureka02.recruitment.dto.response;

import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.entity.Recruitment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 모집 신청 응답  - 성공처리 외에는 db 에 저장할 필요 x
@Getter
@AllArgsConstructor
@Builder
public class RecruitApplyResponse {
    private Long applyId;
    private Long recruitId;

    private RecruitApplyStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;
}
