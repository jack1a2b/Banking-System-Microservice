package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class NotificationService {


    public void alertUser( String accountNumber,String subject, String reason) {
        log.info("------------------------------");
        log.info("Account: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info(reason);
        log.info("------------------------------");
    }
}
