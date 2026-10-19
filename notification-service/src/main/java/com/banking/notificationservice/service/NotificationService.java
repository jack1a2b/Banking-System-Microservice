package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class NotificationService {


    public void alertUser(String transactionId, String accountNumber, BigDecimal amount, String otp, String reason) {
        String VERIFICATION_HEADER = "TRANSACTION VERIFICATION REQUIRED";
        log.info("{}\nFor transaction with id: {}\nAccount number: {}\nAmount: {}\nIf valid enter this six digit OTP: {}\nReason for verificaiton: {}",VERIFICATION_HEADER, transactionId, accountNumber, amount, otp, reason);
    }
}
