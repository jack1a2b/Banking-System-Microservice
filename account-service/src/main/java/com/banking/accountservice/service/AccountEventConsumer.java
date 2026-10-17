package com.banking.accountservice.service;

import com.banking.accountservice.dto.TransactionEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class AccountEventConsumer {

    AccountService accService;


    /**
     * This method consumes transaction.completed events from kafka
     * and credit reciever
     * @param event
     */
    @KafkaListener(topicPattern = "transaction.completed")
    public void consumeTransactionCompleted(@Payload TransactionEvent event){

        try {
            String recieverAccount = event.getAccountNumber();
            BigDecimal amount = event.getAmount();

            accService.creditAccount(recieverAccount, amount);
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
    public void consumeFraudDetection(@Payload Map<String, Object> payload){
          try {
              String accountNumber = payload.get("accountNumber").toString();
              accService.blockAccount(accountNumber);
          } catch (Exception e) {
              log.error("Error blocking account: {}", payload.get("accountNumber").toString());
          }
    }

    /**
     * Consumes account.deduct events from kafka
     * deduct amount from account
     */
    @KafkaListener(topics = "deduct.account")
    public void consumeAccountDeductionEvents(@Payload TransactionEvent event){
        try  {
            accService.deductBalance(event.getAccountNumber(), event.getAmount());
        }catch (Exception e){
            log.error("Error deducting account: {}", event.getAccountNumber());
        }
    }
}
