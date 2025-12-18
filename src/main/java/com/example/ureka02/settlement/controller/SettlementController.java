package com.example.ureka02.settlement.controller;

import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.recruitment.Enum.RecruitMemberRole;
import com.example.ureka02.recruitment.repository.RecruitMemberRepository;
import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.settlement.service.SettlementService;
import com.example.ureka02.user.customUserDetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final RecruitMemberRepository recruitMemberRepository;

    @PostMapping("/recruitments/{recruitmentId}/start")
    public ResponseEntity<Long> startSettlement(
            @PathVariable Long recruitmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1️⃣ 모집 멤버 전체 조회
        List<RecruitmentMember> members =
                recruitMemberRepository.findByRecruitmentId(recruitmentId);

        // 2️⃣ 방장(admin) 찾기
        RecruitmentMember admin = members.stream()
                .filter(m -> m.getRole() == RecruitMemberRole.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("방장이 존재하지 않습니다."));

        // 3️⃣ 요청자가 방장인지 검증
        if (!admin.getMember().getId().equals(userDetails.getId())) {
            throw new IllegalStateException("방장만 정산을 시작할 수 있습니다.");
        }

        // 4️⃣ 총 금액 (임시값, 필요 시 RequestBody로 변경)
        Long totalAmount = 60000L;

        // 5️⃣ 정산 생성
        Settlement settlement =
                settlementService.createSettlement(admin, totalAmount);

        return ResponseEntity.ok(settlement.getId());
    }
}
