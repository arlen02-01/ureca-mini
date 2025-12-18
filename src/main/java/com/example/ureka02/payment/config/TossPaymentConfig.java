package com.example.ureka02.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Getter
public class TossPaymentConfig {

    @Value("${toss.client-key}")
    private String clientKey;

    @Value("${toss.secret-key}")
    private String secretKey;

    public static final String TOSS_API_URL = "https://api.tosspayments.com/v1/payments/";

}