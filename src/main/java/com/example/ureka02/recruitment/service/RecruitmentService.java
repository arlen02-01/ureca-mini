package com.example.ureka02.recruitment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.ureka02.settlement.service.SettlementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ureka02.global.error.CommonException;
import com.example.ureka02.global.error.ErrorCode;
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

    private final SettlementService settlementService;
    private final RecruitRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final RecruitApplyRepository recruitApplyRepository;
    private final RecruitApplyService recruitApplyService;

    private final StringRedisTemplate redisTemplate; // Redis 사용
    private static final String RECRUIT_COUNT_KEY_PREFIX = "recruit:count:";

    // 모집글 생성
    @Transactional
    public RecruitDetailResponse createRecruitment(RecruitCreateRequest request, Long userId) {
        // 작성자 조회
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException(ErrorCode.USER_NOT_FOUND));

        // 미리 신청할 친구들(preAppliers) 조회 및 검증
        List<Long> preApplierIds = request.getPreApplierid();
        List<User> preAppliers = List.of();

        if (preApplierIds != null) {
            preAppliers = userRepository.findAllById(preApplierIds);

            if (preAppliers.size() != preApplierIds.size()) {
                throw new CommonException(ErrorCode.USER_NOT_FOUND);
            }
        }

        int preApplierCnt = preAppliers.size();

        Recruitment recruitment = Recruitment.builder()
                .title(request.getTitle())
                .totalSpots(request.getTotalSpots())
                .description(request.getDescription())
                .endTime(request.getEndTime())
                .creator(creator)
                .build();

        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);

        // 신청 로직을 RecruitApplyService로 위임
        List<RecruitmentApply> savedPreApplierApplies = recruitApplyService.createPreApplies(
                savedRecruitment, preAppliers);

        // 인원수와 상태 최종 업데이트
        savedRecruitment.initializeSpots(preApplierCnt);

        // Redis 카운트 초기화
        String countKey = RECRUIT_COUNT_KEY_PREFIX + savedRecruitment.getId();

        // 상세 조회와 동일하게 미리 신청된 친구들 리스트만 모아서 응답
        List<RecruitApplicationsResponse> applicantResponses = toApplicantResponses(savedPreApplierApplies);
        redisTemplate.opsForValue().set(countKey, String.valueOf(savedRecruitment.getCurrentSpots()));

        return toDetailResponse(savedRecruitment, applicantResponses);
    }

    // 모집글 상세 조회
    public RecruitDetailResponse getRecruitDetails(Long recruitId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitId)
                .orElseThrow(() -> new CommonException(ErrorCode.RECRUITMENT_NOT_FOUND));

        // 이 모집글에 APPLIED 상태로 신청한 사람들 조회 (신청 시간 순서대로)
        List<RecruitmentApply> applies = recruitApplyRepository.findByRecruitmentIdAndStatusOrderByAppliedAtAsc(
                recruitId, RecruitApplyStatus.APPLIED);

        List<RecruitApplicationsResponse> applicantResponses = toApplicantResponses(applies);

        return toDetailResponse(recruitment, applicantResponses);
    }

    // 모집글 목록 조회
    public Page<RecruitListItemResponse> getListRecruitments(int page, int size) {
        // List<Recruitment> recruitments = recruitmentRepository.findAll();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Recruitment> result = recruitmentRepository.findAll(pageable);

        // Entity 객체를 dto 로 변환하기 위해 Page 의 map 사용
        return result.map(this::toListItemResponse);

    }

    // 내가 작성한 모집글 목록 조회
    public Page<RecruitListItemResponse> getMyListItemRecruits(Long createdId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
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
                .createdAt(recruitment.getCreatedAt())
                .endTime(recruitment.getEndTime())
                .currentSpots(recruitment.getCurrentSpots())
                .status(recruitment.getStatus())
                .applications(applications)
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
                .createdAt(recruitment.getCreatedAt())
                .build();
    }


    @Transactional
    public void completeRecruitment(Long recruitmentId, Integer totalAmount) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId).orElseThrow();
        recruitment.complete();
        recruitmentRepository.save(recruitment);

    }

}