package com.example.ureka02.settlement.entity;

import com.example.ureka02.recruitment.entity.Recruitment;
import com.example.ureka02.settlement.enums.SettlementStatus;
import com.example.ureka02.user.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Recruitment recruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private Long totalAmount;

    @Builder
    public Settlement(Recruitment recruitment, User creator,
                      SettlementStatus status, Long totalAmount) {
        this.recruitment = recruitment;
        this.creator = creator;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public void complete() {
        this.status = SettlementStatus.COMPLETED;
    }

    public void changeStatus(SettlementStatus status) {
        this.status = status;
    }


}

