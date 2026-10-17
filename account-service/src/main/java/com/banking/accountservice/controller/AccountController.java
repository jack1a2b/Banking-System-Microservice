package com.banking.accountservice.controller;


import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Repository
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @Autowired
    AccountService accService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request){
        return accService.createAccount(request);
    }


    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        return accService.getAccount(accountNumber);
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return accService.getBalance(accountNumber);
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber){
        return accService.blockAccount(accountNumber);
    }

    /*
     *SAGA STEP1 Deduct Balance
     * this method will be called by the transaction service
     * when transfer is initiated
     * */

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount){
        return accService.deductBalance(accountNumber, amount);
    }

    /**
     * SAGA STEP 4: Compensation Transaction endpiont
     * called by the transaction service
     */

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditAccount(@PathVariable String accountNumber, @RequestParam BigDecimal amount){
        return  accService.creditAccount(accountNumber, amount);
    }

}
