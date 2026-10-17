package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.TransactionServiceClient;
import com.banking.frauddetectionservice.dto.TransactionEvent;
import com.banking.frauddetectionservice.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudService {

    TransactionServiceClient client;

    KafkaTemplate<String, Object> kafkaTemplate;

    public void analyzeTransaction(TransactionEvent event) {
        log.info("Analyzing transaction - account number {}", event.getAccountNumber());

        List<TransactionResponse> responses = client.accountTransactionHistory(event.getAccountNumber());
        /**
         * If accoun is total transaction is more than 100000
         * flagged fraud.detected
         * else transaction.completed
         */
        BigDecimal totalTransactionAmount = new BigDecimal(0);

        for (TransactionResponse response : responses){
            totalTransactionAmount.add(response.getAmount());
        }

        if (totalTransactionAmount.compareTo(BigDecimal.valueOf(100000)) > 0){
            kafkaTemplate.send("transaction.completed",event);
        }else {
            kafkaTemplate.send("fraud.detected", event);
        }


    }
}
