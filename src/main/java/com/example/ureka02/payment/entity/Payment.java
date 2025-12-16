package com.example.ureka02.payment.entity;

import com.example.ureka02.payment.enums.PaymentStatus;
import com.example.ureka02.recruitment.entity.RecruitmentMember;
import com.example.ureka02.settlement.entity.Settlement;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private RecruitmentMember member;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "order_id", unique = true)
    private String orderId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "customer_key")
    private String customerKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;



    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (orderId == null) {
            orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (customerKey == null) {
            customerKey = "user_" + member.getMember().getId();
        }
    }

    public void complete(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }
}