package com.example.ureka02.payment.dto;

import com.example.ureka02.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPaymentStatus {
    private Long memberId;
    private String memberName;
    private String orderId;
    private Integer amount;
    private String status;
    private LocalDateTime paidAt;

    public static MemberPaymentStatus from(Payment payment) {
        return MemberPaymentStatus.builder()
                .memberId(payment.getMember().getId())
                .memberName(payment.getMember().getMember().getName())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .build();
    }
}