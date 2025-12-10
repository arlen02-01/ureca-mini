package com.example.ureka02.recruitment.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor

public class RecruitCompletedResponse {

    private Long recruitId;
    private String status;
    private List<RecruitMemberResponse> members;
}
