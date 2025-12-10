package com.example.ureka02.recruitment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.recruitment.dto.request.RecruitCreateRequest;
import com.example.ureka02.recruitment.dto.response.MyAppliedRecruitResponse;
import com.example.ureka02.recruitment.dto.response.RecruitApplyResponse;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.service.RecruitApplyService;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Recruit Apply API", description = "밥친구 모집글에 대한 신청 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "recruitments/apply")
public class RecruitApplyController {
    /*
     * private final RecruitApplyService recruitApplyService;
     * 
     * // 모집 신청
     * 
     * @PostMapping("/{recruitmentId}")
     * 
     * @Operation(summary = "선착순 모집글 신청", description = "사용자는 특정 모집글에 신청할 수 있습니다.")
     * public ResponseEntity<ResponseDto<RecruitApplyResponse>> applyRecruitment(
     * 
     * @PathVariable Long recruitmentId,
     * 
     * @AuthenticationPrincipal CustomUserDetails principal) {
     * 
     * Long applierId = principal.getId();
     * 
     * RecruitApplyResponse response =
     * recruitApplyService.(recruitmentId,applierId);
     * 
     * return
     * ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.ok(response));
     * }
     * 
     * // 내가 신청한 모집글 목록
     * 
     * @GetMapping("/my/ApplyList")
     * 
     * @Operation(summary = "내가 신청한 모집글 목록", description =
     * "현재 로그인한 사용자가 신청한 모집글을 조회합니다. ")
     * public ResponseEntity<ResponseDto<MyAppliedRecruitResponse>>
     * getMyAplliedRecruits(
     * 
     * @AuthenticationPrincipal CustomUserDetails principal,
     * 
     * @RequestParam(defaultValue = "0") int page,
     * 
     * @RequestParam(defaultValue = "5") int size) {
     * 
     * Long applierId = principal.getId();
     * 
     * MyAppliedRecruitResponse response =
     * recruitApplyService.getMyAplliedRecruits();
     * 
     * return ResponseEntity.ok(ResponseDto.ok(response));
     * }
     * 
     */

}
