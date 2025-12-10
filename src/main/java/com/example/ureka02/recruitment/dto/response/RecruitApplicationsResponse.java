package com.example.ureka02.recruitment.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 신청 인원 조회
@Getter
@Builder
@AllArgsConstructor
public class RecruitApplicationsResponse {
    private Long userId;
    private String name;

    private LocalDateTime appliedAt;

    // 신청한 순서
    private int order;
}
