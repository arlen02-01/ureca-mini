package com.example.ureka02.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.user.User;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모집 작성 엔티티
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    private String title; // 모집글 제목
    private String description; // 내용 작성

    private int totalSpots; // 최대 인원
    private int currentSpots; // 현재 신청한 인원
    private LocalDateTime endTime; // 마감 시간 설정

    @Enumerated(EnumType.STRING)
    private RecruitStatus status; // 모집 상태

    private LocalDateTime createdAt;

    /*
     * // 모집 마감일 설정 - 시간 지나서 자동 마감까지느 배치/스케줄러 추후 구현.
     * public boolean isExpired() {
     * return LocalDateTime.now().isAfter(this.endTime);
     * }
     */

    public void initializeSpots(int preApplierCount) {
        this.currentSpots = preApplierCount + 1; // 작성자 포함.

        // 인원 충족 시 close() 도메인 메서드 호출
        if (this.currentSpots >= this.totalSpots) {
            close();
        }
    }

    /*
     * public void increaseCurrentSpots() { // 신청
     * this.currentSpots++;
     * if (this.currentSpots >= this.totalSpots) {
     * close();
     * }
     * }
     */

    public void decrementCurrentSpots() { // 취소
        if (this.currentSpots > 0) {
            this.currentSpots -= 1;
        }
    }

    // 모집 마감 처리 (인원 자동으로 차면 자동마감 )
    public void close() {
        if (this.status == RecruitStatus.OPEN) {
            this.status = RecruitStatus.CLOSED;
        }
    }

    // 모집 완료 (사용자 수동 처리 : 모임 끝난 후 or 인원은 다 안채워졌지만 모집 마감.)
    public void complete() {
        this.status = RecruitStatus.COMPLETED;
    }

    @Builder
    public Recruitment(String title, String description, int totalSpots, LocalDateTime endTime, User creator) {
        this.title = title;
        this.description = description;
        this.totalSpots = totalSpots;

        this.endTime = endTime;
        this.creator = creator;

        this.currentSpots = 0;
        this.createdAt = LocalDateTime.now();
        this.status = RecruitStatus.OPEN;
    }

}
