package com.banking.accountservice.repository;

import com.banking.accountservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AccountRepo extends JpaRepository<Account, String> {
    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    Account findByAccountNumber(String accountNumber);

}
