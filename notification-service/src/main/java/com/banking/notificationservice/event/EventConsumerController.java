package com.banking.notificationservice.event;


import com.banking.notificationservice.dto.OtpGeneratedEventDTO;
import com.banking.notificationservice.dto.TransactionCompletedEvent;
import com.banking.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Slf4j
@Component
public class EventConsumerController {

    @Autowired
    NotificationService notificationService;

    @KafkaListener(topics = "otp.generated")
    public void consumeOtpGenerated(@Payload OtpGeneratedEventDTO eventDTO){

        try {
            String accountNumber = eventDTO.getAccountNumber();
            String otp = eventDTO.getOtp();
            BigDecimal amount = eventDTO.getAmount();
            String transactionId = eventDTO.getTransactionId();
            String reason = eventDTO.getReason();

            String subject = "TRANSACTION VERIFICATION REQUIRED";
            String message = "Suspicious activity detected on your account" +
                            "\nReason: " + reason +
                            "\nA transaction of D" + amount + "is pending verification. \n" +
                            "Your OTP is: " + otp + ". Valid for 5 minutes. \n" +
                            "If this wasn't you - ignore this message.";
            notificationService.alertUser(accountNumber, subject, message);

        }catch (Exception e){
            log.error("Error sending otp notification to user: {}", e.getMessage());
        }

    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload TransactionCompletedEvent event){
        try {
            String senderAccount = event.getSenderAccountNumber();
            String recieverAccount = event.getRevieverAccountNumber();
            String transactionId = event.getTransactionid();
            BigDecimal amount = event.getAmount();
            String subject = "DEBIT ALERT";
            String message = "D" + amount + " debited from account: " + senderAccount;
            notificationService.alertUser(senderAccount, subject, message);
            log.info("Debited sender account with account number: {}", senderAccount);
            notificationService.alertUser(recieverAccount, subject, message);
            log.info("Account credited from account number: {}", senderAccount);

        }catch (RuntimeException e){
            log.error("Error handing transaction event in EventConsumerController " + e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload OtpGeneratedEventDTO event){
        try {
            String accountNumber = event.getAccountNumber();
            BigDecimal amount = event.getAmount();
            String reason = event.getReason();

            String subject = "FRAUD DETECTED";
            String message = "Fraud/Malicious activity detected on your account\n" +
                    "Reason: " + reason + "\n" +
                    "Amount: " + amount;
            notificationService.alertUser(accountNumber, subject, message);
        } catch (Exception e) {
            log.error("Error sending fraud detection alert to the user {}", e.getMessage() );
            throw new RuntimeException(e);
        }
    }

}
