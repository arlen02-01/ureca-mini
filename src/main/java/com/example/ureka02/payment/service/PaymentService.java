package com.example.ureka02.payment.service;

import com.example.ureka02.payment.client.TossPaymentClient;
import com.example.ureka02.payment.dto.PaymentSuccessDto;
import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.repository.PaymentRepository;
import com.example.ureka02.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final SettlementService settlementService;

    @Transactional
    public Payment confirmPayment(String paymentKey, String orderId, Integer amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다."));

        // 토스 API 호출 (실패해도 무조건 성공 처리)
        tossPaymentClient.confirmPayment(paymentKey, orderId, amount);

        // 결제 완료 처리
        payment.complete(paymentKey);
        Payment savedPayment = paymentRepository.save(payment);

        // 정산 진행 상태 업데이트
        settlementService.updateSettlementProgress(payment.getSettlement().getId());

        log.info("결제 완료 - Payment ID: {}, Member: {}", savedPayment.getId(), payment.getMember().getMember().getName());
        return savedPayment;
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제를 찾을 수 없습니다."));
    }
}