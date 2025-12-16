package com.example.ureka02.settlement.controller;

import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 정산 시작 (팀장이 정산하기 버튼 클릭)
     * POST /api/settlements/{settlementId}/start
     */
    @PostMapping("/{settlementId}/start")
    public ResponseEntity<?> startSettlement(@PathVariable Long settlementId) {
        log.info("정산 시작 요청 - settlementId: {}", settlementId);

        settlementService.startSettlement(settlementId);

        Settlement settlement = settlementService.getSettlement(settlementId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "정산이 시작되었습니다.");
        response.put("settlementId", settlementId);
        response.put("status", settlement.getStatus());
        response.put("payments", settlement.getPayments());

        return ResponseEntity.ok(response);
    }

    /**
     * 정산 조회
     * GET /api/settlements/{settlementId}
     */
    @GetMapping("/{settlementId}")
    public ResponseEntity<Settlement> getSettlement(@PathVariable Long settlementId) {
        Settlement settlement = settlementService.getSettlement(settlementId);
        return ResponseEntity.ok(settlement);
    }

    /**
     * 모집글별 정산 조회
     * GET /api/settlements/recruitment/{recruitmentId}
     */
    @GetMapping("/recruitment/{recruitmentId}")
    public ResponseEntity<Settlement> getSettlementByRecruitment(@PathVariable Long recruitmentId) {
        Settlement settlement = settlementService.getSettlementByRecruitment(recruitmentId);
        return ResponseEntity.ok(settlement);
    }
}