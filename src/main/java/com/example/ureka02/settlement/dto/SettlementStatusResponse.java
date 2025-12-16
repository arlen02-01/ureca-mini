package com.example.ureka02.settlement.dto;

import com.example.ureka02.payment.dto.MemberPaymentStatus;
import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.settlement.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementStatusResponse {
    private Long settlementId;
    private Long recruitmentId;
    private String recruitmentTitle;
    private SettlementStatus status;
    private Integer totalAmount;
    private Integer amountPerPerson;
    private List<MemberPaymentStatus> memberPayments;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static SettlementStatusResponse from(Settlement settlement) {
        List<MemberPaymentStatus> paymentStatuses = settlement.getPayments().stream()
                .map(MemberPaymentStatus::from)
                .collect(Collectors.toList());

        return SettlementStatusResponse.builder()
                .settlementId(settlement.getId())
                .recruitmentId(settlement.getRecruitment().getId())
                .recruitmentTitle(settlement.getRecruitment().getTitle())
                .status(settlement.getStatus())
                .totalAmount(settlement.getTotalAmount())
                .amountPerPerson(settlement.getAmountPerPerson())
                .memberPayments(paymentStatuses)
                .createdAt(settlement.getCreatedAt())
                .completedAt(settlement.getCompletedAt())
                .build();
    }
}