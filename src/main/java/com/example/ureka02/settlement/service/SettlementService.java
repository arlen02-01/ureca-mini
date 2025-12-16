package com.example.ureka02.settlement.service;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.enums.PaymentStatus;
import com.example.ureka02.payment.repository.PaymentRepository;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.recruitment.repository.RecruitMemberRepository;
import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.settlement.enums.SettlementStatus;
import com.example.ureka02.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final RecruitMemberRepository recruitMemberRepository;

    /**
     * 모집 완료 시 자동으로 정산 생성
     */
    @Transactional
    public Settlement createSettlementAuto(Recruitment recruitment, Integer totalAmount) {
        // 이미 존재하는지 확인
        if (settlementRepository.findByRecruitment(recruitment).isPresent()) {
            log.warn("정산이 이미 존재합니다. Recruitment ID: {}", recruitment.getId());
            return settlementRepository.findByRecruitment(recruitment).get();
        }

        List<RecruitmentMember> members = recruitMemberRepository.findByRecruitment(recruitment);
        int memberCount = members.size();

        if (memberCount == 0) {
            throw new IllegalStateException("멤버가 없어 정산을 생성할 수 없습니다.");
        }

        int amountPerPerson = totalAmount / memberCount;

        Settlement settlement = Settlement.builder()
                .recruitment(recruitment)
                .totalAmount(totalAmount)
                .amountPerPerson(amountPerPerson)
                .status(SettlementStatus.PENDING)
                .build();

        Settlement savedSettlement = settlementRepository.save(settlement);
        log.info("정산 자동 생성 - Settlement ID: {}, Recruitment ID: {}, 총 금액: {}원, 인원: {}명",
                savedSettlement.getId(), recruitment.getId(), totalAmount, memberCount);

        return savedSettlement;
    }

    /**
     * 정산 시작 (팀장이 정산하기 버튼 클릭)
     */
    @Transactional
    public void startSettlement(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));

        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new IllegalStateException("정산을 시작할 수 없는 상태입니다.");
        }

        List<RecruitmentMember> members = recruitMemberRepository.findByRecruitment(settlement.getRecruitment());

        if (members.isEmpty()) {
            throw new IllegalStateException("멤버가 없어 정산을 시작할 수 없습니다.");
        }

        for (RecruitmentMember member : members) {
            Payment payment = Payment.builder()
                    .settlement(settlement)
                    .member(member)
                    .amount(settlement.getAmountPerPerson())
                    .status(PaymentStatus.PENDING)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            settlement.addPayment(savedPayment);

            log.info("결제 요청 생성 - Member: {}, Amount: {}원",
                    member.getMember().getName(), settlement.getAmountPerPerson());
        }

        settlement.start();
        settlementRepository.save(settlement);

        log.info("정산 시작 완료 - Settlement ID: {}, 총 {}명의 멤버에게 결제 요청 생성",
                settlementId, members.size());
    }

    /**
     * 결제 완료 시 정산 진행 상태 업데이트
     */
    @Transactional
    public void updateSettlementProgress(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));

        int completedCount = settlement.getCompletedPaymentCount();
        int totalCount = settlement.getTotalPaymentCount();
        double progressPercentage = totalCount > 0 ? (completedCount * 100.0) / totalCount : 0;

        log.info("결제 진행률 - Settlement ID: {}, {}/{} ({}%)",
                settlement.getId(), completedCount, totalCount, String.format("%.1f", progressPercentage));

        // 모든 멤버가 결제 완료했으면 정산 완료 처리
        if (settlement.isAllPaid()) {
            settlement.checkAndComplete();
            settlementRepository.save(settlement);
            log.info("🎉 정산 완료! Settlement ID: {}", settlement.getId());
        }
    }

    @Transactional(readOnly = true)
    public Settlement getSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Settlement getSettlementByRecruitment(Long recruitmentId) {
        // 실제로는 Recruitment 엔티티가 필요하지만, 여기서는 간단하게 처리
        return settlementRepository.findAll().stream()
                .filter(s -> s.getRecruitment().getId().equals(recruitmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
    }
}