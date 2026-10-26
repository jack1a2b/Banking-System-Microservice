package com.banking.accountservice.controller;


import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.dto.TransactionDto;
import com.banking.accountservice.model.Account;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequestMapping("/api/v1/accounts")
@RestController
public class AccountController {

    @Autowired
    AccountService accService;

    /**
     * Tested and proven
     * @param request
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request){
        return accService.createAccount(request);
    }

    @GetMapping("/")
    public String home(){
        return "Account Service";
    }

    /**
     * Tested and proven
     * @param accountNumber
     * @return
     */
    @GetMapping("/{accountNumber}/account")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber){
        return accService.getAccount(accountNumber);
    }

    /**
     * Tested and proven
     * @param accountNumber
     * @return
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return accService.getBalance(accountNumber);
    }

    /**
     * Tested and proven
     * @param accountNumber
     * @return
     */
    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber){
        return accService.blockAccount(accountNumber);
    }

    /**
     * Tested and proven
     * @return
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAccounts(){
        return accService.getAllAccounts();
    }
    /*
     *SAGA STEP1 Deduct Balance
     * this method will be called by the transaction service
     * when transfer is initiated
     * */


    /**
     * Tested and proven
     * @return
     */
    @PostMapping("/deduct")
    public ResponseEntity<String> deductBalance(@RequestBody TransactionDto request){
        return accService.deductBalance(request.getAccountNumber(), request.getAmount());
    }

    /**
     * SAGA STEP 4: Compensation Transaction endpiont
     * called by the transaction service
     */

    /**
     * Tested and proven
     * @return
     */
    @PutMapping("/credit")
    public ResponseEntity<String> creditAccount(@RequestBody TransactionDto request){
        return  accService.creditAccount(request.getAccountNumber(), request.getAmount());
    }

    /**
     * This method will be called by the transaction service to
     * Check balance before publishing an event to deduct balance
     */

    /**
     * Tested and proven
     * @return
     */
    @PostMapping("/check-balance")
    public ResponseEntity<Boolean> checkBalance(@RequestBody TransactionDto request){
        log.info("checking balance for account number: {}\nTo deduct amount {}", request.getAccountNumber(), request.getAmount());
        return accService.checkBalance(request.getAccountNumber(), request.getAmount());
    }

}
