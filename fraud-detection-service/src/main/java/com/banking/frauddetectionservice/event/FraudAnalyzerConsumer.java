package com.banking.frauddetectionservice.event;


import com.banking.frauddetectionservice.service.FraudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FraudAnalyzerConsumer {

    FraudService service;
    public FraudAnalyzerConsumer(FraudService service){
        this.service = service;
    }

    /**
     * This event is publish by the transaction by initiating the transaction
     * In the processTransaction method
     * @param event
     */
    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-service-group")
    public void analyzeTransaction(@Payload Map<String, Object> event){
        log.info("Analyzing fraud");
        service.analyzeTransaction(event);
    }

}
