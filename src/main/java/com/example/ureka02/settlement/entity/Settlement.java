package com.example.ureka02.settlement.entity;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.settlement.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false, unique = true)
    private Recruitment recruitment;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "amount_per_person", nullable = false)
    private Integer amountPerPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void addPayment(Payment payment) {
        this.payments.add(payment);
        payment.setSettlement(this);
    }

    public int getCompletedPaymentCount() {
        return (int) payments.stream().filter(Payment::isCompleted).count();
    }

    public int getTotalPaymentCount() {
        return payments.size();
    }

    public boolean isAllPaid() {
        return !payments.isEmpty() && payments.stream().allMatch(Payment::isCompleted);
    }

    public void checkAndComplete() {
        if (isAllPaid()) {
            this.status = SettlementStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void start() {
        this.status = SettlementStatus.IN_PROGRESS;
    }
}