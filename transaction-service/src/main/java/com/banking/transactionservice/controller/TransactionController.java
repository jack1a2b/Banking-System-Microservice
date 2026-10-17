package com.banking.transactionservice.controller;


import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @Autowired
    TransactionService transService;

    /**
     * This method gets all the transaction
     * @return
     */
    @GetMapping("/")
    public ResponseEntity<List<TransactionResponse>> getAllTransaction(){
        return transService.getAllTransaction();
    }

    /**
     * This method gets you just one transaction with transaction id
     * @param transactionId
     * @return
     */

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId){
        return transService.getTransaction(transactionId);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> accountTransactionHistory(@PathVariable @Valid String accountNumber){
        return transService.getAccountTransactionHis(accountNumber);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionRequest> processTransaction(@Valid @RequestBody TransactionRequest request){
        return transService.processTransaction(request);
    }
//    TO BE IMPLEMENTED LATTER
//    @PostMapping("/{transactionId}/verify")
//    public ResponseEntity<TransactionResponse> verifyOTP(@PathVariable String transactionId, @RequestParam String otp){
//        log.info("verification otp sent - transaction: {}", transactionId);
//        return transService.verifyOTP(transactionId, otp);
//    }
}
