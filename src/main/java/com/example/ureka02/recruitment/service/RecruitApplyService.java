package com.example.ureka02.recruitment.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ureka02.global.config.RedissionConfig;
import com.example.ureka02.recruitment.dto.response.RecruitDetailResponse;
import com.example.ureka02.recruitment.repository.RecruitApplyRepository;
import com.example.ureka02.recruitment.repository.RecruitRepository;

import lombok.RequiredArgsConstructor;

// 모집 신청 처리
// 사용자가 모집글에 신청할 때, Redis를 사용하여 인원 수를 체크하고,
// Redisson으로 중복 신청 방지를 처리

@Service
@RequiredArgsConstructor
public class RecruitApplyService {

    private final RecruitRepository recruitRepository;
    private final RecruitApplyRepository recruitApplyRepository;
    // private final UserRepository userRepository;

    @Transactional
    public void applyRecruitment(Long recruitmentId, Long userId) {
        // 1. 분산락 획득

        // 2. 모집글 조회

        // 3. 중복 신청 방지

        // 4. 모집 인원 증가

        // 5. 신청 정보 저장

    }

    // 내가 신청한 모집리스트 조회
    /*
     * public Page<MyAppliedRecruitResponse> getMyAplliedRecruits() {
     * 
     * }
     */

}
