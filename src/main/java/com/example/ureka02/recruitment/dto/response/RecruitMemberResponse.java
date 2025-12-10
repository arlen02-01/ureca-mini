package com.example.ureka02.recruitment.dto.response;

import com.example.ureka02.recruitment.Enum.RecruitMemberRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 모집완료시 확정 멤버
@Getter
@Builder
@AllArgsConstructor
public class RecruitMemberResponse {
    private Long memberId;
    private Long userId;
    private String name;
    private RecruitMemberRole role;
}
