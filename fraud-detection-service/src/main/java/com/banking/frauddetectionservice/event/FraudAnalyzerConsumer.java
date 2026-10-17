package com.banking.frauddetectionservice.event;


import com.banking.frauddetectionservice.dto.TransactionEvent;
import com.banking.frauddetectionservice.service.FraudService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class FraudAnalyzerConsumer {

    FraudService service;

    @KafkaListener(topics = "transaction.initiated")
    public void analyzeTransaction(@Payload TransactionEvent event){
        service.analyzeTransaction(event);
    }

}
