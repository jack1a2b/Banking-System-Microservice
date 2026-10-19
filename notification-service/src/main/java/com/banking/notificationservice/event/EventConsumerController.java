package com.banking.notificationservice.event;


import com.banking.notificationservice.dto.OtpGeneratedEventDTO;
import com.banking.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class EventConsumerController {

    @Autowired
    NotificationService notificationService;
    @KafkaListener(topics = "otp.generated")
    public void consumeOtpGenerated(@Payload OtpGeneratedEventDTO eventDTO){

        String accountNumber = eventDTO.getAccountNumber();
        String otp = eventDTO.getOtp();
        BigDecimal amount = eventDTO.getAmount();
        String transactionId = eventDTO.getTransactionId();
        String reason = eventDTO.getReason();

        notificationService.alertUser(transactionId, accountNumber, amount, otp, reason);
    }

}
