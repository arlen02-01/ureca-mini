package com.example.ureka02.recruitment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.ureka02.recruitment.entity.Recruitment;

import io.lettuce.core.dynamic.annotation.Param;

public interface RecruitRepository extends JpaRepository<Recruitment, Long> {

    // 내가 작성한 모집글 목록
    Page<Recruitment> findByCreatorId(Long creatorId, Pageable pagble);

    // DB의 원자적 업데이트를 통해 currentSpots를 증가시키는 메서드
    // 이 쿼리는 current_spots가 total_spots보다 작을 때만 업데이트를 수행
    @Modifying
    @Query("UPDATE Recruitment r SET r.currentSpots = r.currentSpots + 1 " +
            "WHERE r.id = :recruitmentId AND r.currentSpots < r.totalSpots")
    int incrementCurrentSpotsAtomic(@Param("recruitmentId") Long recruitmentId);
    // 증가 처리 성공시 1 반환
}