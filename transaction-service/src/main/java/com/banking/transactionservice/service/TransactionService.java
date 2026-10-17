package com.banking.transactionservice.service;


import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.model.TransactionType;
import com.banking.transactionservice.repository.TransactionRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionRepo transRepo;

    private AccountServiceClient client;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";


    public ResponseEntity<List<TransactionResponse>> getAllTransaction() {
        try {
            List<TransactionResponse> transactions = transRepo.findAllTransations();
            return new ResponseEntity<>(transactions, HttpStatus.OK);
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
            List<TransactionResponse> responses = generateTransactionResponse(transactions);
            return new ResponseEntity<>(responses, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Problem with transaction by account: {} ", accountNumber);
            throw new RuntimeException(e);
        }

    }



    public ResponseEntity<TransactionRequest> processTransaction(@Valid TransactionRequest request) {
        log.info("SAGA STEP 1 - Transfer: {} -> {}\nAmount {} ",request.getSenderAccountNumber(), request.getReceiverAccountNumber(), request.getAmount());

        try {
            client.deductBalance(request.getSenderAccountNumber(), request.getAmount());
        }catch (RuntimeException e){
            log.error("Problem calling the deduct method using the account service client");
            throw new RuntimeException("Problem calling the deduct method using the account service client " + e.getMessage());
        }

        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        try {
            Transaction savedTransaction = transRepo.save(transaction);
            log.info("transaction saved as processing {}", savedTransaction.getId());

            TransactionInitiatedEvent event = new TransactionInitiatedEvent();
            event.setTransactionId(savedTransaction.getId());
            event.setAmount(savedTransaction.getAmount());
            event.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
            event.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());

            TransactionRequest transactionRequest = new TransactionRequest();


            transactionRequest.setAmount(savedTransaction.getAmount());
            transactionRequest.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
            transactionRequest.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());
            transactionRequest.setDescription(savedTransaction.getDescription());

            kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, event.getTransactionId(), event);
            log.info("Transaction initiated event published");
            return new ResponseEntity<>(transactionRequest, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Error saving the transaction in the database");
            throw new RuntimeException(e);
        }

    }

//    TO BE IMPLEMENTED LATTER
//    public ResponseEntity<TransactionResponse> verifyOTP(String transactionId, String otp) {
//
//    }



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
}
