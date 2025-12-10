package com.example.ureka02.recruitment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.dto.request.RecruitCreateRequest;
import com.example.ureka02.recruitment.dto.response.RecruitApplicationsResponse;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.dto.response.RecruitListItemResponse;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.entity.RecruitmentApply;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;

import lombok.RequiredArgsConstructor;

// 모집글 생성/조회/관리

/*
1. 모집글 생성
2.1 모집글 목록 조회 - 페이징 처리해서 조회
2.2 모집글 상세 조회 - recruitmentId 를 통해 상세 정보 조회
*/
@Service
@RequiredArgsConstructor
public class RecruitmentService {
    private final RecruitRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final RecruitApplyRepository recruitApplyRepository;

    // 모집글 생성
    public RecruitDetailResponse createRecruitment(RecruitCreateRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        // + RuntimeException 대신 CommonException + ErrorCode 로 변경

        Recruitment recruitment = Recruitment.builder()
                .title(request.getTitle())
                .totalSpots(request.getTotalSpots())
                .description(request.getDescription())
                .endTime(request.getEndTime())
                .creator(creator)
                .build();

        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);

        return toDetailResponse(savedRecruitment, List.of());
    }

    // 모집글 상세 조회
    public RecruitDetailResponse getRecruitDetails(Long recruitId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitId)
                .orElseThrow(() -> new RuntimeException("모집글이 존재하지 않습니다."));

        // 이 모집글에 APPLIED 상태로 신청한 사람들 조회 (신청 시간 순서대로)
        List<RecruitmentApply> applies = recruitApplyRepository.findByRecruitmentIdAndStatusOrderByAppliedAtAsc(
                recruitId, RecruitApplyStatus.APPLIED);

        List<RecruitApplicationsResponse> applicantResponses = toApplicantResponses(applies);

        return toDetailResponse(recruitment, applicantResponses);
    }

    // 모집글 목록 조회
    public Page<RecruitListItemResponse> getListRecruitments(int page, int size) {
        // List<Recruitment> recruitments = recruitmentRepository.findAll();
        Pageable pageable = PageRequest.of(page, size);
        Page<Recruitment> result = recruitmentRepository.findAll(pageable);

        // Entity 객체를 dto 로 변환하기 위해 Page 의 map 사용
        return result.map(this::toListItemResponse);

    }

    // 내가 작성한 모집글 목록 조회
    public Page<RecruitListItemResponse> getMyListItemRecruits(Long createdId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Recruitment> result = recruitmentRepository.findByCreatorId(createdId, pageable);
        return result.map(this::toListItemResponse);
    }

    /* 추후에 팩토리 메서드로 리팩토링 예정 */
    // 컨버트 메서드

    private RecruitDetailResponse toDetailResponse(Recruitment recruitment,
            List<RecruitApplicationsResponse> applications) {
        return RecruitDetailResponse.builder()
                .id(recruitment.getId())
                .title(recruitment.getTitle())
                .creatorName(recruitment.getCreator().getName())
                .description(recruitment.getDescription())
                .totalSpots(recruitment.getTotalSpots())
                .endTime(recruitment.getEndTime())
                .currentSpots(recruitment.getCurrentSpots())
                .status(recruitment.getStatus())
                .apllications(applications)
                .build();
    }

    // RecruitmentApply 리스트를 신청자 DTO 리스트로 변환
    private List<RecruitApplicationsResponse> toApplicantResponses(List<RecruitmentApply> applies) {
        List<RecruitApplicationsResponse> list = new ArrayList<>();

        for (int i = 0; i < applies.size(); i++) {
            RecruitmentApply apply = applies.get(i);
            User user = apply.getApplier(); // User 엔티티 (닉네임, 이름 등 조회 가능)

            RecruitApplicationsResponse dto = RecruitApplicationsResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .order(i + 1) // 1번째, 2번째, 3번째...
                    .build();

            list.add(dto);
        }
        return list;
    }

    private RecruitListItemResponse toListItemResponse(Recruitment recruitment) {
        return RecruitListItemResponse.builder()
                .id(recruitment.getId())
                .title(recruitment.getTitle())
                .creatorId(recruitment.getCreator().getId())
                .creatorName(recruitment.getCreator().getName())
                .totalSpots(recruitment.getTotalSpots())
                .currentSpots(recruitment.getCurrentSpots())
                .endTime(recruitment.getEndTime())
                .status(recruitment.getStatus())
                .build();
    }

}
