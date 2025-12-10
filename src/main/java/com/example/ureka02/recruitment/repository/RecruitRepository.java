package com.example.ureka02.recruitment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ureka02.recruitment.entity.Recruitment;

public interface RecruitRepository extends JpaRepository<Recruitment, Long> {

    // 내가 작성한 모집글 목록
    Page<Recruitment> findByCreatorId(Long creatorId, Pageable pagble);
}