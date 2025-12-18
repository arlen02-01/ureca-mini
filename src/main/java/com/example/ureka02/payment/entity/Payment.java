package com.example.ureka02.payment.entity;

import com.example.ureka02.payment.enums.PaymentStatus;
import com.example.ureka02.settlement.entity.Settlement;
import com.example.ureka02.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Long amount;


    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public static Payment create(User user, Settlement settlement, Long amount) {
        Payment payment = new Payment();
        payment.user = user;
        payment.settlement = settlement;
        payment.amount = amount;
        payment.status = PaymentStatus.REQUESTED;
        return payment;
    }
}
