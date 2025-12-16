package com.example.ureka02.recruitment.repository;

import java.util.List;

import com.example.ureka02.recruitment.entity.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ureka02.recruitment.entity.RecruitmentMember;

public interface RecruitMemberRepository extends JpaRepository<RecruitmentMember, Long> {
    List<RecruitmentMember> findByRecruitmentId(Long recruitmentId);

    // 경윤 추가
    List<RecruitmentMember> findByRecruitment(Recruitment recruitment);

}