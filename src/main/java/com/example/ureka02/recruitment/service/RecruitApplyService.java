package com.example.ureka02.recruitment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

/*
Redis INCR는 안전하지만,
DB에서 currentSpots++를 락/버전 없이 처리해서
여러 트랜잭션이 서로 덮어써 Lost Update가 발생했음. -> JPQL 쿼리로 DB 원자적 저장 보장
*/

@Service
@RequiredArgsConstructor
public class RecruitApplyService {
    private final RecruitRepository recruitRepository;
    private final RecruitApplyRepository recruitApplyRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String RECRUIT_COUNT_KEY_PREFIX = "recruit:count:";

    // 미리 신청한 친구들의 신청 기록을 생성하고 저장
    @Transactional
    public List<RecruitmentApply> createPreApplies(Recruitment recruitment, List<User> preAppliers) {
        if (preAppliers == null || preAppliers.isEmpty()) {
            return List.of();
        }

        List<RecruitmentApply> preList = preAppliers.stream().map(applier -> RecruitmentApply.builder()
                .recruitment(recruitment).applier(applier).build()).collect(Collectors.toList());

        return recruitApplyRepository.saveAll(preList);
    }

    @Transactional
    public RecruitApplyResponse applyRecruitment(Long recruitmentId, Long userId) {
        // 1. Redis 로 원자적 동시성 제어(선착순 안에 들기 - 자리 확보)

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
            // recruitment.increaseCurrentSpots();
            int updatedRows = recruitRepository.incrementCurrentSpotsAtomic(recruitmentId);

            if (updatedRows == 0) {
                // 이 경우는 Redis는 통과했지만, DB 쿼리의 WHERE 조건(currentSpots < totalSpots)에 막힌 것.
                // 즉, 다른 트랜잭션들이 먼저 커밋하여 정원이 꽉 찬 상황.
                throw new CommonException(ErrorCode.RECRUITMENT_FULL);
            }
            // 업데이트 성공 시 updatedRows == 1

            // 7. 신청 정보 저장
            RecruitmentApply recruitmentApply = RecruitmentApply.builder()
                    .recruitment(recruitment)
                    .applier(applier)
                    .build();

            RecruitmentApply savedApply = recruitApplyRepository.save(recruitmentApply);

            // 트랜잭션이 성공적으로 커밋되면 (신청 기록 저장 + 인원수 DB 반영 완료)
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
                .endTime(application.getRecruitment().getEndTime())
                .recruitStatus(application.getRecruitment().getStatus())
                .totalSpots(application.getRecruitment().getTotalSpots())
                .currentSpots(application.getRecruitment().getCurrentSpots())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .applyStatus(application.getStatus())
                .build();
    }

}
