package com.banking.accountservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Service
public class AccountEventConsumer {

    AccountService accService;


    /**
     * This method consumes transaction.completed events from kafka
     * and credit reciever
     * @param event
     */
    @KafkaListener(topicPattern = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> event){

        try {
            String receiverAccount = event.get("receiverAccountNumber").toString();
            BigDecimal amount = new BigDecimal(event.get("amount").toString());
            accService.creditAccount(receiverAccount, amount);
        } catch (Exception e) {
            log.error("Error crediting account {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Consumes fraud.detected events from kafka
     * Blocks the flagged account
     */

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetection(@Payload Map<String, Object> event){
        log.info("Fraud detected blocking account: {}\nWith transactionId: {}", event.get("accountNumber"), event.get("transactionId"));
          try {
              String accountNumber = event.get("accountNumber").toString();
              accService.blockAccount(accountNumber);
          } catch (Exception e) {
              log.error("Error blocking account: {}", event.get("accountNumber"));
          }
    }

    /**
     * Consumes account.deduct events from kafka
     * deduct amount from account
     * this method is deprecated for now using the client with openFeign to call the account service synchronously to deduct amount
     */
    @KafkaListener(topics = "deduct.account")
    public void consumeAccountDeductionEvents(@Payload Map<String, Object> event){
        try  {
            String accountNumber = event.get("accountNumber").toString();
            BigDecimal amount = (BigDecimal) event.get("amount");

            accService.deductBalance(accountNumber, amount);
        }catch (Exception e){
            log.error("Error deducting account: {}, Cause by: {}", event.get("accountNumber"), e.getMessage());
        }
    }

}
