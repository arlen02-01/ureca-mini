package com.example.ureka02.recruitment.service;

import java.time.LocalDateTime;

import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ureka02.global.error.CommonException;
import com.example.ureka02.global.error.ErrorCode;
import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.dto.response.MyAppliedRecruitResponse;
import com.example.ureka02.recruitment.dto.response.RecruitApplyResponse;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.entity.RecruitmentApply;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;
import com.example.ureka02.user.User;
import com.example.ureka02.user.UserRepository;

import lombok.RequiredArgsConstructor;

// 모집 신청 처리
// 사용자가 모집글에 신청할 때, Redis를 사용하여 인원 수를 체크하고,
// Redisson으로 중복 신청 방지를 처리 -> 분산락은 추후 구현

@Service
@RequiredArgsConstructor
public class RecruitApplyService {
    private final RecruitRepository recruitRepository;
    private final RecruitApplyRepository recruitApplyRepository;
    private final UserRepository userRepository;
    // private final RedisTemplate<String, String> redisTemplate;
    private final StringRedisTemplate redisTemplate;
    private static final String RECRUIT_COUNT_KEY_PREFIX = "recruit:count:";

    @Transactional
    public RecruitApplyResponse applyRecruitment(Long recruitmentId, Long userId) {
        // 1. Redis 로 원자적 동시성 제어
        // 특정 모집글에 대한 Redis 키 생성
        String countKey = RECRUIT_COUNT_KEY_PREFIX + recruitmentId;
        // INCR 명령어를 실행 - 원자적 증가 가능
        Long currentCount = redisTemplate.opsForValue().increment(countKey);

        try {
            // 2. 사용자 및 모집글 정보 조회
            User applier = userRepository.findById(userId)
                    .orElseThrow(() -> new CommonException(ErrorCode.USER_NOT_FOUND));

            Recruitment recruitment = recruitRepository.findById(recruitmentId)
                    .orElseThrow(() -> new CommonException(ErrorCode.RECRUITMENT_NOT_FOUND));

            // 3. 정원 초과 체크 (Redis 카운트 기반)
            if (currentCount > recruitment.getTotalSpots()) {
                throw new CommonException(ErrorCode.RECRUITMENT_FULL);
            }

            // 4. 마감 시간 체크 (DB 정보 기반)
            LocalDateTime now = LocalDateTime.now();

            if (recruitment.getEndTime() != null && recruitment.getEndTime().isBefore(now)) {
                throw new CommonException(ErrorCode.RECRUITMENT_EXPIRED);
            }

            // 5. 중복 신청 방지
            boolean alreadyApplied = recruitApplyRepository.existsByRecruitmentIdAndApplierIdAndStatus(
                    recruitmentId, userId, RecruitApplyStatus.APPLIED);
            if (alreadyApplied) {
                throw new CommonException(ErrorCode.ALREADY_APPLIED);
            }

            // 6. Recruitment 엔티티에 인원수 증가 (도메인 메서드)
            recruitment.increaseCurrentSpots();

            // 7. 신청 정보 저장
            RecruitmentApply recruitmentApply = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(applier)
                    .build();
            RecruitmentApply savedApply = recruitApplyRepository.save(recruitmentApply);

            return toApplyResponse(savedApply);

        } catch (CommonException e) {
            // 실패 시 Redis 카운터 복구 (DECR)
            redisTemplate.opsForValue().decrement(countKey);
            throw e;
        }

    }

    // 내가 신청한 모집리스트 조회
    public Page<MyAppliedRecruitResponse> getMyAppliedRecruits(Long applierId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        Page<RecruitmentApply> applyPage = recruitApplyRepository.findByApplierId(applierId, pageable);

        // DTO 변환 및 반환
        return applyPage.map(this::toMyAppliedRecruitResponse);
    }

    // 컨버터 메소드 작성
    private RecruitApplyResponse toApplyResponse(RecruitmentApply application) {
        return RecruitApplyResponse.builder()
                .applyId(application.getId())
                .recruitId(application.getRecruitment().getId())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .build();
    }

    private MyAppliedRecruitResponse toMyAppliedRecruitResponse(RecruitmentApply application) {
        return MyAppliedRecruitResponse.builder()
                .applyId(application.getId())
                .recruitId(application.getRecruitment().getId())
                .title(application.getRecruitment().getTitle())
                .recruitStatus(application.getRecruitment().getStatus())
                .totalSpots(application.getRecruitment().getTotalSpots())
                .currentSpots(application.getRecruitment().getCurrentSpots())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .applyStatus(application.getStatus())
                .build();
    }

}
