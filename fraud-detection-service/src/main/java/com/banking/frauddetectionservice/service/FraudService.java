package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.client.TransactionServiceClient;
import com.banking.frauddetectionservice.dto.TransactionEvent;
import com.banking.frauddetectionservice.dto.TransactionResponse;
import com.banking.frauddetectionservice.dto.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudService {
    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";

    TransactionServiceClient transClient;
    AccountServiceClient accClient;
    FraudCheck fraudCheck;

    private  final KafkaTemplate<String, Object> kafkaTemplate;

    public void analyzeTransaction(TransactionEvent event) {


        ResponseEntity<BigDecimal> accClientBalance = accClient.getBalance(event.getAccountNumber());

        BigDecimal accountBalance = accClientBalance.getBody();


        log.info("Checking transaction: {}\nAccount: {}\nAmount: {}\nBalance: {}", event.getTransactionId(), event.getAccountNumber(), event.getAmount(), accountBalance);

        List<TransactionResponse> responses = transClient.accountTransactionHistory(event.getAccountNumber());

        FraudCheckResult result = fraudCheck.performFraudChecks(event.getAccountNumber(), responses, event.getAmount(), accountBalance);

        if(result.isFraud()){
            event.setFraud(true);
            event.setReason(result.getReason());
            log.info("Suspicious activities detected - account: {}\nReason: {}\nRequesting OTP verification", event.getAccountNumber(), event.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, event.getTransactionId(), event);
        }else{
            log.info("Transaction clean");
            event.setReason(null);
            event.setFraud(false);
            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, event);
        }
        


    }
}
