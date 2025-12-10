package com.example.ureka02.recruitment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.recruitment.entity.RecruitmentApply;
import java.util.List;

public interface RecruitApplyRepository extends JpaRepository<RecruitmentApply, Long> {

    // 특정 모집글에 대해 해당 유저가 이미 신청했는지 여부 (중복 신청 방지)
    boolean existsByRecruitmentIdAndApplierIdAndStatus(Long recruitmentId, Long applierId, RecruitApplyStatus status);

    // 내가 신청한 모집글 조회
    Page<RecruitmentApply> findByApplierId(Long applierId, Pageable pageable);

    // 특정 모집글에 신청 승인된 유저 조회
    List<RecruitmentApply> findByRecruitmentIdAndStatus(Long recruitmentId, RecruitApplyStatus status);

    // 특정 모집글에 대해, APPLIED 상태인 신청들만 적용 시간 순으로 조회
    List<RecruitmentApply> findByRecruitmentIdAndStatusOrderByAppliedAtAsc(
            Long recruitmentId,
            RecruitApplyStatus status);

}