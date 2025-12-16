package com.example.ureka02.recruitment.dto.response;

import java.util.List;

import com.example.ureka02.recruitment.Enum.RecruitStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 모집 완료된 전체 결과
@Getter
@Builder
@AllArgsConstructor
public class RecruitCompletedResponse {
    private Long recruitId;
    private RecruitStatus status;
    private List<RecruitMemberResponse> members;
}
