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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member;

    @Enumerated(EnumType.STRING)
    private RecruitMemberRole role;

    @Builder
    public RecruitmentMember(Recruitment recruitment, User member, RecruitMemberRole role) {
        this.recruitment = recruitment;
        this.member = member;
        this.role = role;
    }

}