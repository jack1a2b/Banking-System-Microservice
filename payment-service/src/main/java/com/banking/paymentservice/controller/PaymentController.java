package com.banking.paymentservice.controller;


import com.banking.paymentservice.dto.CreatePaymentRequestDto;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("api/v1/paymants")
public class PaymentController {

    PaymentService paymentService;

    @PostMapping("/make-mayments")
    public ResponseEntity<PaymentOrderResponse> createPaymentRequest(@RequestBody CreatePaymentRequestDto request) throws RazorpayException {
        return paymentService.handlePaymentRequest(request);
    }

    @PostMapping("/webhook")
    public  ResponseEntity<String> handleWebhook (@RequestBody Map<String, Object> payload){
        return paymentService.handleWebhook(payload);
    }
}
