package com.banking.transactionservice.client;

import com.banking.transactionservice.dto.TransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Component
@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PostMapping("/api/v1/accounts/deduct")
    String deductBalance(@RequestBody TransactionDto request);

    @PostMapping("/api/v1/accounts/credit")
    ResponseEntity<String> creditAccount(@RequestBody Map<String, Object> request);

    @GetMapping("/api/v1/accounts/check-balance")
    ResponseEntity<Boolean> checkBalance(@RequestBody TransactionDto request);

    @PutMapping("/api/v1/accounts/{accountNumber}/block")
    String blockAccount(@PathVariable String accountNumber);
}
