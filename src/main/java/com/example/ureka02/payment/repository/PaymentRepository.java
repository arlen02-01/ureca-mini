package com.example.ureka02.payment.repository;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySettlement(Settlement settlement);
    Optional<Payment> findByOrderId(String orderId);
}