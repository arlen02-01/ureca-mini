package com.example.ureka02.recruitment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitApplyStatus;
import com.example.ureka02.user.User;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모집 신청 엔티티
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentApply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applier_id")
    private User applier; //

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id")
    private Recruitment recruitment;

    @Enumerated(EnumType.STRING)
    private RecruitApplyStatus status;

    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;

    // 신청 취소
    public void cancel() {
        if (this.status != RecruitApplyStatus.APPLIED) {
            throw new IllegalStateException("APPLIED 상태에서만 취소할 수 있습니다.");
        } else {
            this.status = RecruitApplyStatus.CANCELED;
            this.canceledAt = LocalDateTime.now();
        }
    }

    // 중복 신청 방지 - 엔티티 메소드에서 할지 서비스에서 할지? -> 서비스에서 작성함.
    @Builder
    public RecruitmentApply(Recruitment recruitment, User applier) {
        this.recruitment = recruitment;
        this.applier = applier;

        this.status = RecruitApplyStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
        this.canceledAt = null;
    }

}
