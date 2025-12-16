package com.example.ureka02.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessDto {
    private String paymentKey;
    private String orderId;
    private String orderName;
    private String method;
    private String totalAmount;
    private String status;
}