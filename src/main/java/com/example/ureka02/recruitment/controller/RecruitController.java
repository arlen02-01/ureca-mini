package com.example.ureka02.recruitment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityReturnValueHandler;

import com.example.ureka02.global.common.ResponseDto;
import com.example.ureka02.recruitment.dto.request.RecruitCreateRequest;
import com.example.ureka02.recruitment.dto.response.MyAppliedRecruitResponse;
import com.example.ureka02.recruitment.dto.response.RecruitCompletedResponse;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.dto.response.RecruitListItemResponse;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.service.RecruitApplyService;
import com.example.ureka02.recruitment.service.RecruitMemberService;
import com.example.ureka02.recruitment.service.RecruitmentService;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Recruit API", description = "밥친구 모집 관련 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/recruitments")
public class RecruitController {

    private final RecruitmentService recruitmentService;
    private final RecruitMemberService recruitMemberService;

    @PostMapping("/posts")
    @Operation(summary = "선착순 밥친구 모집글 작성", description = "모집자가 인원수와 마감일을 설정하여 모집글을 작성합니다")
    public ResponseEntity<ResponseDto<RecruitDetailResponse>> createRecruiment(
            @RequestBody @Valid RecruitCreateRequest request,
            // @AuthenticationPrincipal CustomUserDetails principal
            @RequestParam Long creatorId) {

        // JWT 토큰에서 userId 추출
        // Long creatorId = principal.getId();

        RecruitDetailResponse response = recruitmentService.createRecruitment(request, creatorId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.ok(response));
    }

    // 모집 완료
    @PostMapping("{recruitId}/complete")
    @Operation(summary = "모집 완료 상태로 변경", description = "모집글의 상태를 COMPLETED 로 변경하고 멤버를 생성합니다.")
    public ResponseEntity<ResponseDto<RecruitCompletedResponse>> completeRecruitment(@PathVariable Long recruitId,
            // @AuthenticationPrincipal CustomUserDetails principal
            @RequestParam Long creatorId) {

        // Long creatorId = principal.getId();

        RecruitCompletedResponse response = recruitMemberService.completeRecruitment(recruitId, creatorId);

        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.ok(response));

    }

    // 모집글 상세 조회
    @GetMapping("/{recruitId}")
    @Operation(summary = "모집글 상세 조회", description = "postId를 기반으로 특정 모집글 내용을 조회합니다.")
    public ResponseEntity<ResponseDto<RecruitDetailResponse>> getRecruitDetail(@PathVariable Long recruitId) {
        RecruitDetailResponse response = recruitmentService.getRecruitDetails(recruitId);

        return ResponseEntity.ok(ResponseDto.ok(response));
    }

    // 모집글 목록 조회 + 페이징
    @GetMapping("/lists")
    @Operation(summary = "모집글 목록 조회", description = "모든 모집글을 페이징 목록으로 조회합니다.")
    public ResponseEntity<ResponseDto<Page<RecruitListItemResponse>>> getListRecruitment(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(value = "size", defaultValue = "10") int size) {

        Page<RecruitListItemResponse> responses = recruitmentService.getListRecruitments(page, size);

        return ResponseEntity.ok(ResponseDto.ok(responses));
    }

    // 내가 작성한 모집글 조회
    @GetMapping("my/posts")
    @Operation(summary = "내가 작성한 모집글 목록", description = "현재 로그인한 사용자가 작성한 모집글을 조회합니다. ")
    public ResponseEntity<ResponseDto<Page<RecruitListItemResponse>>> getMyListRecruitment(
            // @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "5") @RequestParam(defaultValue = "5") int size) {

        // Long userId = principal.getId();

        Page<RecruitListItemResponse> response = recruitmentService.getMyListItemRecruits(userId, page, size);

        return ResponseEntity.ok(ResponseDto.ok(response));
    }

}