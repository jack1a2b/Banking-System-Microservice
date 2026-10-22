package com.banking.accountservice.dto;

import com.banking.accountservice.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account holder email is required")
    private String email;

    @NotBlank(message = "Account holder phone is required")
    private String phone;

    @NotNull(message = "daily transaction limit is required")
    private BigDecimal dailyTransactionLimit;

    @NotNull(message = "AccounType is required")
    private AccountType accountType;

    @NotNull(message = "initial deposit is required")
    @Positive(message = "Initial deposit of a minimum amount of D200 must be deposited")
    private BigDecimal initialDeposit;
}
