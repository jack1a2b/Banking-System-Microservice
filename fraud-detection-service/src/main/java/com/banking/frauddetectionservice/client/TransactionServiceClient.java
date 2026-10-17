package com.banking.frauddetectionservice.client;

import com.banking.frauddetectionservice.dto.TransactionResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface TransactionServiceClient {

    @GetMapping("/api/v1/transactions/account/{accountNumber}")
     List<TransactionResponse> accountTransactionHistory( @PathVariable @Validated String accountNumber);
}
