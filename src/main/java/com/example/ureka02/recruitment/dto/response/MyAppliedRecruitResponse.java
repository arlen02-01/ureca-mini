package com.example.ureka02.recruitment.dto.response;

import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.Enum.RecruitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

//내가 신청한 모집글 조회용 dto
@Getter
@AllArgsConstructor
@Builder
public class MyAppliedRecruitResponse {
    private Long applyId;
    private Long recruitId;
    private String title;

    private int totalSpots;
    private int currentSpots;
    private LocalDateTime endTime;
    private RecruitStatus recruitStatus;

    private RecruitApplyStatus applyStatus;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;

}
