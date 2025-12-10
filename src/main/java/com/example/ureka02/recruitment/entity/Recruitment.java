package com.example.ureka02.recruitment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.ureka02.recruitment.Enum.RecruitStatus;
import com.example.ureka02.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // 모집 마감일 설정
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.endTime);
    }

    /*
     * TODO 신청 인원 증가 -> 동시성 제어는 service에서 Redisson + MySQL 트랜잭션으로 처리할건데..엔티티 쪽에서 순수
     * 행위만 처리? 해야하는지? -> 우선 작성함 increaseCurrentSpot
     */
    //

    public void increaseCurrentSpots() {
        this.currentSpots++;
        if (this.currentSpots >= this.totalSpots) {
            close();
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

    /*
     * TODO 미리 친구 추가가 된 인원을 모임에 추가? 까지 가져갈 것인지 - 이건 추후 협의 후 작성
     */

    @Builder
    public Recruitment(String title, String description, int totalSpots, LocalDateTime endTime, User creator) {
        this.title = title;
        this.description = description;
        this.totalSpots = totalSpots;
        this.endTime = endTime;
        this.creator = creator;

        this.currentSpots = 0; // 친구 추가 여부에 따라 빌더로 받을지 결정
        this.status = RecruitStatus.OPEN;
    }
}
