package com.banking.notificationservice.event;

import com.banking.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;


@Slf4j
@Component
public class EventConsumerController {

    @Autowired
    NotificationService notificationService;

    @KafkaListener(topics = "otp.generated")
    public void consumeOtpGenerated(@Payload Map<String, Object> eventDTO){

        try {
            String accountNumber = eventDTO.get("senderAccountNumber").toString();
            String otp = eventDTO.get("otp").toString();
            BigDecimal amount = new BigDecimal(eventDTO.get("amount").toString());
            String transactionId = eventDTO.get("transactionId").toString();
            String reason = eventDTO.get("reason").toString();

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
    public void consumeTransactionCompleted(@Payload Map<String, Object> event){
        log.info("Sending alerts to the user {}", event);
        try {
            String senderAccount = event.get("senderAccountNumber").toString();
            String recieverAccount = event.get("receiverAccountNumber").toString();
            String transactionId = event.get("transactionId").toString();
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

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
    public void consumeFraudDetected(@Payload Map<String, Object> event){
        try {
            String accountNumber = event.get("accountNumber").toString();
            BigDecimal amount = new BigDecimal(event.get("amount").toString());
            String reason = event.get("reason").toString();

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
    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(@Payload Map<String, Object> event){
        try {
            String accountNumber = event.get("accountNumber").toString();
            BigDecimal amount = new BigDecimal(event.get("amount").toString());
            String reason = event.get("reason").toString();
            String transactionId = event.get("transactionId").toString();

            String subject = "REFUND PROCESSED";
            String message = "Your transaction with transactionId: " + transactionId + ", was cancelled!!\n"+
                    "Reason: " + reason + "\n" +
                    "Amount: " + amount;
            notificationService.alertUser(accountNumber, subject, message);
        } catch (Exception e) {
            log.error("Error sending transaction refunded alert to the user {}", e.getMessage() );
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(@Payload Map<String, Object> payload){
        try {
            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();

            String subject = "PAYMENT SUCCESSFUL!!";
            String message = "Payment of D" + amount + " completed. \n"
                            + "Razorpay ID: " + payload.get("razorpayPaymentId");
            notificationService.alertUser(accountNumber, subject, message);

        }catch (Exception e){
            log.error("Error handling payment notification to user");
            throw new RuntimeException(e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed")
    public void consumeFailedPaymentEvent(@Payload Map<String, Object> payload){

        try {

            String accountNumber = payload.get("accountNumber").toString();
            String amount = payload.get("amount").toString();
            String reason = payload.get("reason").toString();

            String subject = "PAYMENT FAILED";
            String message = "Payment of D" + amount + " Failed\n"
                            + "Reason: " + reason + "\n"
                            + "You are account will shortly be refunded";

            notificationService.alertUser(accountNumber, subject, message);

        }catch (Exception e){
            log.error("Error handling payment failed notification to alert user about the failure");
            throw new RuntimeException(e.getMessage());
        }

    }


}
