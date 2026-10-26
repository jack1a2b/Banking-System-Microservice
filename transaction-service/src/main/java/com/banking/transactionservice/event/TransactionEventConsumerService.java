package com.banking.transactionservice.event;
import com.banking.transactionservice.dto.TransactionEvent;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepo;
import com.banking.transactionservice.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;


@Data
@Slf4j
@Service
public class TransactionEventConsumerService {

    private TransactionService transactionService;
    private TransactionRepo transRepo;
    private long otpExpirationDuration = 5;

    public TransactionEventConsumerService(TransactionService transactionService, TransactionRepo transRepo, StringRedisTemplate redisTemplate, KafkaTemplate<String, Object> kafkaTemplate) {
        this.transactionService = transactionService;
        this.transRepo = transRepo;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    StringRedisTemplate redisTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;


    /**
     * This event is published by the TransactionEventConsumerSerivce class in the transaction service
     * To verify the transation with OTP
     */
    @KafkaListener(topics = "verification.required", groupId = "transaction-service-group")
    public void consumeVerification(@Payload Map<String, Object> event){
        log.warn("Verification required for transaction: {}, account: {}, amount: {}\nReason: {}", event.get("transactionId"), event.get("senderAccountNumber"), event.get("amount"), event.get("reason"));
        String transactionId = event.get("transactionId").toString();
        String reason = event.get("reason").toString();
        String accountNumber = event.get("senderAccountNumber").toString();
        BigDecimal amount = (BigDecimal) event.get("amount");


        Transaction transaction = transRepo.findById(transactionId).get();

        if(transaction.getStatus() != TransactionStatus.PROCESSING){
            log.warn("Problem with the transaction status of transaction: {}, has a transaction status of: {}", transactionId, transaction.getStatus());
            throw new RuntimeException("Theres is a problem with transaction status");
        }

        String otp = String.format("06d", new SecureRandom().nextInt(1_000_000));

        String otpKey = "verification:otp" + transactionId;


        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(otpExpirationDuration));

        transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
        transaction.setFailureReason(reason);
        transRepo.save(transaction);

        log.info("OTP generated of transaction: {}, And expired in {} minutes", transactionId ,otpExpirationDuration);

        event.put("otp", otp);


        /**
         * This event is sent to the notification service to notify or send the OTP to the user
         * For verification
         */
        kafkaTemplate.send("otp.generated",event.get("transactionId").toString(), event);

    }

    /**
     * This event is published by the fraud detection service in the FraudService
     * After the fraud check passed
     * @param event
     */
    @KafkaListener(topics = "fraud.check.clean",groupId = "transaction-service-group")
    public void consumeFraudClean(@Payload Map<String, Object> event){
        log.info("{}",event);
        String transactionId = event.get("transactionId").toString();
        transactionService.processFraudCheckClean(transactionId);

    }
}
