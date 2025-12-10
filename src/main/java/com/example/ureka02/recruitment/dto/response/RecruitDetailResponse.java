package com.example.ureka02.recruitment.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.recruitment.entity.Recruitment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 모집글 작성 응답 + 상세 조회용 
@Getter
@Builder
@AllArgsConstructor
public class RecruitDetailResponse {
    private Long id;
    private String title;
    private String description;

    private Long creatorId;
    private String creatorName;

    private int totalSpots;
    private int currentSpots;
    private LocalDateTime endTime;
    private RecruitStatus status;

    // 현재 모집 중인 신청자 목록
    private List<RecruitApplicationsResponse> apllications;
}
