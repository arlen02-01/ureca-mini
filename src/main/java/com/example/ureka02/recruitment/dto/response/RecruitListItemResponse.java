package com.example.ureka02.recruitment.dto.response;

import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 모집글 목록 조회용
@Getter
@AllArgsConstructor
@Builder
public class RecruitListItemResponse {
    private Long id;
    private String title;

    private Long creatorId;
    private String creatorName;

    private int totalSpots;
    private int currentSpots;
    private LocalDateTime endTime;
    private RecruitStatus status;
}
