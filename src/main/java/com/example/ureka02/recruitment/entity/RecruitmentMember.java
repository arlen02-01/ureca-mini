package com.example.ureka02.recruitment.entity;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.recruitment.Enum.RecruitMemberRole;
import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.user.User;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id")
    private Recruitment recruitment;

    // private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member; // 지금은 단순 pk 만 필요하지만 추후 pay 와 합칠때 및 유지보수를 생각해 User 로 매핑

    @Enumerated(EnumType.STRING)
    private RecruitMemberRole role;

    @Builder
    public RecruitmentMember(Recruitment recruitment, User member, RecruitMemberRole role) {
        this.recruitment = recruitment;
        this.member = member;
        this.role = role;
    }

}