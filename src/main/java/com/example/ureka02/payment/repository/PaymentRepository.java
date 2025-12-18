package com.example.ureka02.payment.repository;

import com.example.ureka02.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySettlementId(Long settlementId);
}
