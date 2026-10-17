package com.banking.accountservice.dto;


import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.model.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private String id;

    private String accountNumber;

    private String accountHolderName;

    private String email;

    private String phone;

    private AccountType accountType;

    private AccountStatus status;

    private BigDecimal balance;

    private LocalDateTime createdAt;

    private BigDecimal dalyTransactionLimit;

    private LocalDateTime updatedAt;


}
