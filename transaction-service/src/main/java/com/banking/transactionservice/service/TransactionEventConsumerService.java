package com.banking.transactionservice.service;


import com.banking.transactionservice.dto.TransactionEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEventConsumerService {

    TransactionService transactionService;


    //Method to be completed
//    @KafkaListener(topics = "transaction.completed")
//    public void consumeTransactionCompleted(@Payload TransactionEvent event){
//        log.info("Updating the transaction status to completed");
//        String accountNumber = event.getAccountNumber();
//        transactionService.updateTransactionStatus(accountNumber);
//    }
}
