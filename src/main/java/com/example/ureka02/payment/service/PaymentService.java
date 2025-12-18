package com.example.ureka02.payment.service;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.enums.PaymentStatus;
import com.example.ureka02.payment.repository.PaymentRepository;
import com.example.ureka02.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SettlementService settlementService;

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 없음"));
    }

    @Transactional
    public void confirmSuccess(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 없음"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS);

        settlementService.checkAndComplete(
                payment.getSettlement().getId()
        );
    }
}
