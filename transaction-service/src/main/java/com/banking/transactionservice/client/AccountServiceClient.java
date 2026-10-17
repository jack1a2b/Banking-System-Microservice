package com.banking.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PutMapping("/api/v1/accounts/{accountNumber}/deduct")
    String deductBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount);

    @PutMapping("{/api/v1/accounts/{accountNumber}/credit")
    ResponseEntity<String> creditAccount(@PathVariable String accountNumber, @RequestParam BigDecimal amount);

    @GetMapping("/api/v1/accounts/{accountNumber}/check-balance")
    ResponseEntity<Boolean> checkBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount);
}
