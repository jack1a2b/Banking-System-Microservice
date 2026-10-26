package com.banking.accountservice.service;


import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.model.Account;
import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.model.AccountType;
import com.banking.accountservice.repository.AccountRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountService {
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    AccountRepo accRepo;

    public ResponseEntity<AccountResponse> createAccount(@Valid CreateAccountRequest request) {
        log.info("Creating account:{}", request.getEmail());
        if(accRepo.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account already exists for email: " + request.getEmail());
        }
        Account account = createAccountHandler(request);
        Account savedAccount = accRepo.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        AccountResponse response = createResponseHandler(savedAccount);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }


    private String generateAccountNumber() {
       String accountNumber;
       do{
           long number = secureRandom.nextLong(1_000_000_000_000L);
           accountNumber = String.format("%012d", number);
       }while (accRepo.existsByAccountNumber(accountNumber));

       return accountNumber;
    }


    public ResponseEntity<AccountResponse> getAccount(String accountNumber) {
        log.info("Fetching with account number: {}", accountNumber);
        try {
            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account not found");
            }
            Account account = accRepo.findByAccountNumber(accountNumber);

            AccountResponse response = createResponseHandler(account);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            throw  new RuntimeException("Account not found " + e.getMessage());
        }
    }

    public ResponseEntity<BigDecimal> getBalance(String accountNumber) {
        try {

            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account number not found");
            }

            Account account = accRepo.findByAccountNumber(accountNumber);
            return new ResponseEntity<>(account.getBalance(), HttpStatus.OK);
        } catch (RuntimeException e) {
            throw new RuntimeException("Account not found");
        }
    }



    /**
     * Block account - called by Fraud detection service via kafka
     * @param accountNumber
     * @return
     */
    public ResponseEntity<String> blockAccount(String accountNumber) {
        try {
            log.info("Blocking account: {}", accountNumber);

            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account number not found");
            }

            Account account = accRepo.findByAccountNumber(accountNumber);
            account.setStatus(AccountStatus.BLOCKED);

            log.info("Account of {}, with account# {} Blocked ", account.getAccountHolderName(), account.getAccountNumber());
            accRepo.save(account);
            return new ResponseEntity<>("Account: " + account.getAccountNumber() + "Blocked Successfully", HttpStatus.OK);
        }catch(RuntimeException e){
            throw new RuntimeException("Account not found");
        }

    }



    /**
     * Deduct Balance - called by the transaction service
     *                  to deduct user account balance
     * @param accountNumber
     * @param amount
     * @return
     */
    public ResponseEntity<String> deductBalance(String accountNumber, BigDecimal amount) {
        try {
            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account number not exist");
            }

            Account account = accRepo.findByAccountNumber(accountNumber);
            log.info("deducting account: {}\nBalance before deduction {}", accountNumber, account.getBalance() );


            if(account.getBalance().compareTo(amount) < 0){
                log.info("Your account Balance is: {}", account.getBalance());
                return new ResponseEntity<>("You have insufficient balance in your account", HttpStatus.NOT_ACCEPTABLE);
            } else if (account.getStatus() != AccountStatus.ACTIVE) {
                log.info("Account is not active please visit the nearest branch");
                return new ResponseEntity<>("Your account is not active please visit the nearest branch", HttpStatus.NOT_ACCEPTABLE);
            }

            account.setBalance(account.getBalance().subtract(amount));
            accRepo.save(account);
            log.info("Balance after deduction: {}", account.getBalance());

            return new ResponseEntity<>("Account deducted successfully!", HttpStatus.OK);
        }catch (RuntimeException e){
            throw new RuntimeException("Account not found");
        }
    }



    /**
     * Credit balance - called by the transaction service
     *                  to credit user account balance
     * @param accountNumber
     * @param amount
     * @return
     */
    public ResponseEntity<String> creditAccount(String accountNumber, BigDecimal amount) {
        try {
            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account number not exist");
            }


            Account account = accRepo.findByAccountNumber(accountNumber);
            log.info("Crediting account: {}\nBalance before crediting {}", accountNumber, account.getBalance() );
            account.setBalance(account.getBalance().add(amount));

            Account savedAccount = accRepo.save(account);

            log.info("Credited account: {}\nBalance after crediting {}", accountNumber, savedAccount.getBalance() );
            return  new ResponseEntity<>("Account credited successfully", HttpStatus.OK);
        }catch (RuntimeException e){
            throw new RuntimeException("Account not credited");
        }
    }


    public ResponseEntity<Boolean> checkBalance(String accountNumber, BigDecimal amount) {
        try {
            Account account = accRepo.findByAccountNumber(accountNumber);
            if(!accRepo.existsByAccountNumber(accountNumber)){
                throw new RuntimeException("Account not found for account number: " + accountNumber);
            }
            if(account.getBalance().compareTo(amount) > 0){
                return new ResponseEntity<>(true, HttpStatus.OK);
            }

            return new ResponseEntity<>(false, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Error checking account balance: {}\nTo deduct amount: {}", accountNumber, amount);
            throw new RuntimeException(e);
        }

    }

    private AccountResponse createResponseHandler(Account savedAccount) {
        AccountResponse response = new AccountResponse();
        response.setId(savedAccount.getId());
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setAccountHolderName(savedAccount.getAccountHolderName());
        response.setAccountType(savedAccount.getAccountType());
        response.setEmail(savedAccount.getEmail());
        response.setPhone(savedAccount.getPhone());
        response.setStatus(savedAccount.getStatus());
        response.setBalance(savedAccount.getBalance());
        response.setDalyTransactionLimit(savedAccount.getDailyTransationLimit());
        response.setCreatedAt(savedAccount.getCreatedAt());
        response.setUpdatedAt(savedAccount.getUpdatedAt());

        return response;
    }

    private Account createAccountHandler(@Valid CreateAccountRequest request) {
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(request.getInitialDeposit());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setDailyTransationLimit(
                request.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
        );
        return account;
    }


    public ResponseEntity<List<Account>> getAllAccounts() {
        return new ResponseEntity<>(accRepo.findAll(), HttpStatus.OK);
    }
}
