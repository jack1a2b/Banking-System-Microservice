package com.banking.transactionservice.event;
import com.banking.transactionservice.dto.OtpGeneratedEventDTO;
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

import java.security.SecureRandom;
import java.time.Duration;


@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEventConsumerService {

    private TransactionService transactionService;
    private TransactionRepo transRepo;
    private long otpExpirationDuration = 5;
    StringRedisTemplate redisTemplate;
    KafkaTemplate<String, Object> kafkaTemplate;


    @KafkaListener(topics = "verification.required")
    public void consumeVerification(@Payload TransactionEvent event){
        log.warn("Verification required for transaction: {}, account: {}, amount: {}\nReason: {}", event.getTransactionId(), event.getAccountNumber(), event.getAmount(), event.getReason());
        String transactionId = event.getTransactionId();
        Transaction transaction = transRepo.findById(transactionId).get();

        if(transaction.getStatus() != TransactionStatus.PROCESSING){
            log.warn("Problem with the transaction status of transaction: {}, has a transaction status of: {}", transactionId, transaction.getStatus());
            throw new RuntimeException("Theres is a problem with transaction status");
        }

        String otp = String.format("06d", new SecureRandom().nextInt(1_000_000));

        String otpKey = "verification:otp" + transactionId;


        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(otpExpirationDuration));

        transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
        transaction.setFailureReason(event.getReason());
        transRepo.save(transaction);

        log.info("OTP generated of transaction: {}, And expired in {} minutes", transactionId ,otpExpirationDuration);

        OtpGeneratedEventDTO eventDTO = new OtpGeneratedEventDTO();

        eventDTO.setTransactionId(event.getTransactionId());
        eventDTO.setAccountNumber(event.getAccountNumber());
        eventDTO.setOtp(otp);
        eventDTO.setAmount(event.getAmount());
        eventDTO.setReason(event.getReason());

        kafkaTemplate.send("otp.generated", eventDTO);

    }

    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudClean(@Payload TransactionEvent event){
        transactionService.processFraudCheckClean(event.getTransactionId());
    }
}
