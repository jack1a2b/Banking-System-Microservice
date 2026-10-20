package com.banking.paymentservice.service;


import com.banking.paymentservice.dto.CreatePaymentRequestDto;
import com.banking.paymentservice.dto.PaymentOrderResponse;
import com.banking.paymentservice.model.Payment;
import com.banking.paymentservice.model.PaymentStatus;
import com.banking.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.HTTP;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Data
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    PaymentRepository paymentRepository;

    public ResponseEntity<PaymentOrderResponse> handlePaymentRequest(CreatePaymentRequestDto request) throws RazorpayException {
        log.info("creating payment order for account: {} amount: {}", request.getAccountNumber(),request.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        //converting amount to bututs
        int amountValue = request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountValue);
        orderRequest.put("currency", "GMD");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0,10));

        Order order =  razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created: {}", order.get("id").toString());

        Payment savedPayment = savePayemnt(order, request);
        PaymentOrderResponse response = generateResponse(savedPayment, keyId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    public ResponseEntity<String> handleWebhook(Map<String, Object> payload) {
        log.info("Received Razorpay webhook: {}", payload.get("event"));


        String event = payload.get("event").toString();

        if("payment.captured".equals(event)){
            handlePaymentSuccess(payload);
            return new ResponseEntity<>("Payment handled successfully", HttpStatus.OK);
        }else if("payment.failed".equals(event)){
            handlePaymentFailure(payload);
            return new ResponseEntity<>("Failure encountered check the full stack trace", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("Error handling payment", HttpStatus.BAD_REQUEST);

    }




    private PaymentOrderResponse generateResponse(Payment savedPayment, String keyId) {
        PaymentOrderResponse response = new PaymentOrderResponse();

        response.setAmount(savedPayment.getAmount());
        response.setCurrency(savedPayment.getCurrency());
        response.setPaymentId(savedPayment.getRazorpayOrderId());
        response.setStatus(String.valueOf(savedPayment.getStatus()));
        response.setRazorpayKeyId(keyId);
        return response;
    }

    private Payment savePayemnt(Order order, CreatePaymentRequestDto request) {
        Payment payment = new Payment();
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setRazorpayOrderId(order.get("id").toString());
        payment.setCurrency("GMD");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());
        return paymentRepository.save(payment);
    }


    private void handlePaymentFailure(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);


            String orderId = paymentData.get("order_id").toString();
            try {
                Payment payment = paymentRepository.fineByRazorpayOrId(orderId);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment failed via Razorpay");
                paymentRepository.save(payment);

                //publish payment failure event

                Map<String, Object> event = new HashMap<>();
                event.put("paymentId", payment.getId());
                event.put("accountNumber", payment.getAccountNumber());
                event.put("amount", payment.getAmount());
                event.put("reason", "Payment failed via Razorpay");

                kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getId(), event);
                log.warn("Payment failed: {}", payment.getId());
            } catch (Exception e) {
                log.info("Error handling payment failure {}", e.getMessage());
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void handlePaymentSuccess(Map<String, Object> payload) {
        Map<String, Object> paymentData = extractPaymentData(payload);

        String orderId = paymentData.get("order_id").toString();
        String paymentId = paymentData.get("id").toString();


        try {

            Payment payment = paymentRepository.fineByRazorpayOrId(orderId);
            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);

            paymentRepository.save(payment);

            Map<String, Object> event = new HashMap<>();
            event.put("paymentId", payment.getId());
            event.put("amount", payment.getAmount());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("razorpayPaymentId", payment.getRazorpayPaymentId());

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, payment.getId(), event);
            log.info("Payment competed: {}", payment.getId());

        }catch (RuntimeException e){
            throw new RuntimeException("Payment not found for order: {}" + orderId + "\n" + e.getMessage());
        }
    }

    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {

        Map<String, Object> entity = (Map<String, Object>) payload.get("payload");

        Map<String, Object> paymentWrapper = (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>) paymentWrapper.get("entity");
    }
}
