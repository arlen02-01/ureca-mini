package com.example.ureka02.settlement.repository;

import com.example.ureka02.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByRecruitmentId(Long recruitmentId);
}
