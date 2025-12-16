package com.example.ureka02.payment.controller;

import com.example.ureka02.payment.config.TossPaymentConfig;
import com.example.ureka02.payment.dto.TossConfirmRequest;
import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentConfig tossPaymentConfig;

    /**
     * 토스 결제 페이지
     * GET /payment/toss?paymentId=1
     */
    @GetMapping("/toss")
    public String tosPaymentPage(@RequestParam Long paymentId, Model model) {
        log.info("토스 결제 페이지 로드 - paymentId: {}", paymentId);

        Payment payment = paymentService.getPaymentById(paymentId);

        model.addAttribute("paymentId", paymentId);
        model.addAttribute("orderId", payment.getOrderId());
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("memberName", payment.getMember().getMember().getName());
        model.addAttribute("customerKey", payment.getCustomerKey());
        model.addAttribute("clientKey", tossPaymentConfig.getClientKey());
        model.addAttribute("settlementId", payment.getSettlement().getId());

        return "payment/payment-toss";
    }

    /**
     * 결제 승인
     * POST /api/payments/confirm
     */
    @PostMapping("/api/payments/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmPayment(@RequestBody TossConfirmRequest request) {
        log.info("결제 승인 요청 - orderId: {}", request.getOrderId());
        Payment payment = paymentService.confirmPayment(request.getPaymentKey(), request.getOrderId(), request.getAmount());
        return ResponseEntity.ok(payment);
    }
}