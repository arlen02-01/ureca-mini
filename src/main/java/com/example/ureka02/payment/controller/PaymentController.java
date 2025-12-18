package com.example.ureka02.payment.controller;

import com.example.ureka02.payment.entity.Payment;
import com.example.ureka02.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 정보 조회
    @GetMapping("/api/{paymentId}")
    @ResponseBody
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(Map.of(
                "amount", payment.getAmount(),
                "status", payment.getStatus()
        ));
    }

    // Toss 결제 승인 콜백
    @PostMapping("/api/confirm/{paymentId}")
    @ResponseBody
    public ResponseEntity<?> confirmPayment(@PathVariable Long paymentId) {

        paymentService.confirmSuccess(paymentId);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
