package com.example.ureka02.settlement.service;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.enums.PaymentStatus;
import com.example.ureka02.payment.repository.PaymentRepository;
import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.settlement.enums.SettlementStatus;
import com.example.ureka02.settlement.repository.SettlementRepository;
import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.recruitment.repository.RecruitMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final RecruitMemberRepository recruitMemberRepository;
    private final PaymentRepository paymentRepository;

    // ✅ 기존 코드 그대로 유지
    public Settlement createSettlement(RecruitmentMember admin, Long totalAmount) {

        Settlement settlement = Settlement.builder()
                .recruitment(admin.getRecruitment())
                .creator(admin.getMember())
                .status(SettlementStatus.IN_PROGRESS)
                .totalAmount(totalAmount)
                .build();

        settlementRepository.save(settlement);

        List<RecruitmentMember> members =
                recruitMemberRepository.findByRecruitmentId(
                        admin.getRecruitment().getId()
                );

        int memberCount = members.size();
        long amountPerPerson = totalAmount / memberCount;

        for (RecruitmentMember member : members) {
            Payment payment = Payment.create(
                    member.getMember(),
                    settlement,
                    amountPerPerson
            );
            paymentRepository.save(payment);
        }

        return settlement;
    }

    // ✅ 이 메서드가 없어서 에러가 난 것
    public void checkAndComplete(Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));

        if (settlement.getStatus() == SettlementStatus.COMPLETED) {
            return;
        }

        List<Payment> payments =
                paymentRepository.findBySettlementId(settlementId);

        boolean allSuccess = payments.stream()
                .allMatch(p -> p.getStatus() == PaymentStatus.SUCCESS);

        if (allSuccess) {
            settlement.changeStatus(SettlementStatus.COMPLETED);
        }
    }
}
