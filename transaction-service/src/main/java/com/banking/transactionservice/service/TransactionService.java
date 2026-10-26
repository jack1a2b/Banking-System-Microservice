package com.banking.transactionservice.service;


import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.*;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.model.TransactionType;
import com.banking.transactionservice.repository.TransactionRepo;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionService {

    @Autowired
    private final KafkaTemplate<String, Object> kafkaTemplate;
    //will add redis dependency in my pom file later
    @Autowired
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TransactionRepo transRepo;
    @Autowired
    private AccountServiceClient client;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";


    public ResponseEntity<List<TransactionResponse>> getAllTransaction() {
        try {
            List<Transaction> transactions = transRepo.findAllTransations();
            List<TransactionResponse>  responses = generateTransactionResponse(transactions);

            return new ResponseEntity<>(responses, HttpStatus.OK);
        }catch (Exception e){
            log.error("Error in fetching all transactions");
            throw new RuntimeException(e.getMessage());
        }
    }

    public ResponseEntity<TransactionResponse> getTransaction(String transactionId) {
        try {
            Transaction transaction = transRepo.findById(transactionId).get();
            TransactionResponse response = generateResponse(transaction);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e){
            log.error("Error in fetching transaction by transaction id: {}", e.getMessage());
            throw  new RuntimeException(e.getMessage());
        }
    }


    public ResponseEntity<List<TransactionResponse>> getAccountTransactionHis(@Valid String accountNumber) {
        try {
            List<Transaction> transactions = transRepo.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber);

            //Here am typecasting from type transaction to type transaction response
            //Using generateTransactionResponse
            List<TransactionResponse> responses = generateTransactionResponse(transactions);
            return new ResponseEntity<>(responses, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Problem with transaction by account: {} ", accountNumber);
            throw new RuntimeException(e);
        }

    }



    public ResponseEntity<TransactionRequest> processTransaction(@Valid TransactionRequest request) {
        log.info("SAGA STEP 1 - Transfer: {} -> {}\nAmount {} ",request.getSenderAccountNumber(), request.getReceiverAccountNumber(), request.getAmount());

        TransactionEvent events = new TransactionEvent();
        events.setAccountNumber(request.getSenderAccountNumber());
        events.setAmount(request.getAmount());


        try {

            TransactionDto dto = new TransactionDto(request.getSenderAccountNumber(), request.getAmount());

            ResponseEntity<Boolean> result = client.checkBalance(dto);


            if (Boolean.TRUE.equals(result.getBody())){
                TransactionDto transRequest = new TransactionDto(request.getSenderAccountNumber(), request.getAmount());
//                kafkaTemplate.send("deduct.account", events);
                client.deductBalance(transRequest);

            }else {
                log.info("Amount balance is insufficient");
                return new ResponseEntity<>(new TransactionRequest(), HttpStatus.FORBIDDEN);
            }
        }catch (RuntimeException e){
            log.error("Problem calling the deduct method using the account service client");
            throw new RuntimeException("Problem calling the deduct method using the account service client " + e.getMessage());
        }

        try {

            Transaction transaction = new Transaction();

            transaction.setSenderAccountNumber(request.getSenderAccountNumber());
            transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
            transaction.setType(TransactionType.TRANSFER);
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setReferenceNumber(UUID.randomUUID().toString());

            Transaction savedTransaction = transRepo.save(transaction);
            log.info("transaction saved as processing {}", savedTransaction.getId());

            TransactionInitiatedEvent event = new TransactionInitiatedEvent();

            Map<String, Object> event2 = new HashMap<>();
            event2.put("transactionId", savedTransaction.getId());
            event2.put("amount", savedTransaction.getAmount());
            event2.put("senderAccountNumber", savedTransaction.getSenderAccountNumber());
            event2.put("receiverAccountNumber", savedTransaction.getReceiverAccountNumber());

            event.setTransactionId(savedTransaction.getId());
            event.setAmount(savedTransaction.getAmount());
            event.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
            event.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());


            TransactionRequest transactionRequest = new TransactionRequest();


            transactionRequest.setAmount(savedTransaction.getAmount());
            transactionRequest.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
            transactionRequest.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());
            transactionRequest.setDescription(savedTransaction.getDescription());

            /**
             * This event will consumed by the fraud detection service
             * To check for weather its to publish fraud.detected or fraud.clean event
             * To be consumed by the payment service
             */
            kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,event.getTransactionId(), event2);
            log.info("Transaction initiated event published");
            return new ResponseEntity<>(transactionRequest, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Error saving the transaction in the database");
            throw new RuntimeException(e);
        }

    }

    public ResponseEntity<TransactionResponse> verifyOTP(String transactionId, String otp) {

        String key = "verification:otp" + transactionId;

        Transaction transaction = transRepo.findById(transactionId).get();


        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp == null){
            log.info("OTP already expired");
            transaction.setStatus(TransactionStatus.FLAGGED);
            transaction.setDescription("OTP expired!!");
            transRepo.save(transaction);
            compensateSender(transaction.getSenderAccountNumber(),transactionId, transaction.getAmount());

            Map<String, Object> eventDTO = prepareOtpExpireEvent(transactionId, transaction);

            /**
             * This published event get consumed by the notification service to notify the user about the conpensation
             */
            kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC,eventDTO.get("transactionId").toString(), eventDTO);
            return new ResponseEntity<>(generateResponse(transaction), HttpStatus.NOT_ACCEPTABLE);
        }

        if(!otp.equals(savedOtp)){
            //log incorrect otp block account and flag
            redisTemplate.delete(key);
            Map<String, Object> eventDTO = prepareOtpEvent(transactionId, transaction);

            //Account service listen to this event and blocks the account immediately!!
            kafkaTemplate.send(FRAUD_DETECTED_TOPIC, eventDTO.get("transactionId").toString(), eventDTO);
            //String message = client.blockAccount(transaction.getSenderAccountNumber());
            transaction.setDescription("Account Block kindly visit the nearest branch");
            transaction.setStatus(TransactionStatus.FLAGGED);
            transRepo.save(transaction);
            log.warn("OTP not valid account will be block for security reason. Please visit the nearest branch for review\n{}", transaction.getFailureReason());
            compensateSender(transaction.getSenderAccountNumber(),transactionId, transaction.getAmount());


            kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, eventDTO.get("transactionId").toString(), eventDTO);
            return new ResponseEntity<>(generateResponse(transaction), HttpStatus.NOT_ACCEPTABLE);
        }
        //Credit account
        completeTransaction(transaction);

        return new ResponseEntity<>(generateResponse(transaction), HttpStatus.OK);
    }

    public void processFraudCheckClean(String transactionId) {
        Optional<Transaction> transaction = transRepo.findById(transactionId);

        if(transaction.isEmpty()){
            throw new RuntimeException("Transaction not found: " + transactionId);
        }

        if(transaction.get().getStatus() != TransactionStatus.PROCESSING){
            log.warn("Transaction {} not processing - skipping", transactionId);
            return;
        }

        completeTransaction(transaction.get());

    }

    private void completeTransaction(Transaction transaction) {
        try {
            Map<String, Object> eventDTO =  prepareEvent(transaction);

            TransactionDto request = new TransactionDto(transaction.getReceiverAccountNumber(), transaction.getAmount());
            String message = client.creditAccount(request).getBody();

            transaction.setFailureReason("");
            transaction.setDescription(message);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transRepo.save(transaction);


            kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, eventDTO.get("transactionId").toString(), eventDTO);
        } catch (RuntimeException e) {
            log.info("Error completing the transaction in the complete transation method");
            throw new RuntimeException(e);
        }

    }

    private Map<String, Object> prepareOtpExpireEvent(String transactionId, Transaction transaction) {
        Map<String, Object> eventDTO = new HashMap<>();
        eventDTO.put("transactionId", transactionId);
        eventDTO.put("reason", "Otp already expired");
        eventDTO.put("amount", transaction.getAmount());
        eventDTO.put("accountNumber", transaction.getSenderAccountNumber());
        eventDTO.put("otp", null);
        return eventDTO;
    }


    private Map<String, Object> prepareOtpEvent(String transactionId, Transaction transaction) {

        Map<String, Object> eventDTO = new HashMap<>();
        eventDTO.put("transactionId", transactionId);
        eventDTO.put("reason", "We detected some malicious fraud activities on this account");
        eventDTO.put("amount", transaction.getAmount());
        eventDTO.put("accountNumber", transaction.getSenderAccountNumber());
        eventDTO.put("otp", null);
        return eventDTO;
    }

    private Map<String, Object> prepareEvent(Transaction transaction) {
        Map<String, Object> event = new HashMap<>();
        event.put("amount", transaction.getAmount());
        event.put("senderAccountNumber", transaction.getSenderAccountNumber());
        event.put("receiverAccountNumber", transaction.getReceiverAccountNumber());
        event.put("transactionId", transaction.getId());
        event.put("referenceNumber", transaction.getReferenceNumber());

        return event;
    }

    public void compensateSender(String senderAccountNumber, String transactionId, BigDecimal amount) {
        log.info("Compensating Transaction: {}\nTo account number: {} With amount: {} ", transactionId, senderAccountNumber ,amount );

        TransactionDto request = new TransactionDto(senderAccountNumber, amount);
        client.creditAccount(request);
    }


    private List<TransactionResponse> generateTransactionResponse(List<Transaction> transactions) {
        List<TransactionResponse> responses = new ArrayList<>();
        for(Transaction transaction : transactions){
            TransactionResponse response = new TransactionResponse(
                    transaction.getId(),
                    transaction.getSenderAccountNumber(),
                    transaction.getReceiverAccountNumber(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDescription(),
                    transaction.getFailureReason(),
                    transaction.getReferenceNumber(),
                    transaction.getCreatedAt(),
                    transaction.getCompletedAt()
            );
            responses.add(response);
        }

        return responses;
    }


    private TransactionResponse generateResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();

        response.setId(transaction.getId());
        response.setSenderAccountNumber(transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setFailureReason(transaction.getFailureReason());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());
        return  response;
    }




//    public void updateTransactionStatus(String accountNumber) {
//        Transaction savedTransaction = transRepo.findB
//    }
}
