package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.client.TransactionServiceClient;
import com.banking.frauddetectionservice.dto.TransactionResponse;
import com.banking.frauddetectionservice.dto.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    public void analyzeTransaction(Map<String, Object> event) {
        String senderAccountNumber = event.get("senderAccountNumber").toString();
        BigDecimal amount = (BigDecimal) event.get("amount");
        String transactionId = event.get("transactionId").toString();
        String receiverAccountNumber = event.get("receiverAccountNumber").toString();
        String reason = event.get("reason").toString();



        ResponseEntity<BigDecimal> accClientBalance = accClient.getBalance(senderAccountNumber);

        BigDecimal accountBalance = accClientBalance.getBody();


        log.info("Checking transaction: {}\nAccount: {}\nAmount: {}\nBalance: {}", transactionId, senderAccountNumber, amount, accountBalance);

        List<TransactionResponse> responses = transClient.accountTransactionHistory(senderAccountNumber);

        FraudCheckResult result = fraudCheck.performFraudChecks(senderAccountNumber, responses, amount, accountBalance);

        if(result.isFraud()){
            event.put("isFraud", true);
            event.put("reason", result.getReason());

            log.info("Suspicious activities detected - account: {}\nReason: {}\nRequesting OTP verification", senderAccountNumber,reason);

            /**
             * This event is published when fraud is detected and sends it to the transaction service(In the TransactionEventConsumerService in the events package)
             * To generate the OTP for verification
             */
            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId, event);
        }else{
            log.info("Transaction clean");
            event.put("isFraud", false);
            event.put("reason", null);

            /**
             * This event is published a fraud clean check and sends it to the  transaction service(In the TransactionEventConsumerService in the events package)
             * To process fraud check clean
             */
            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId, event);
        }
        


    }
}
