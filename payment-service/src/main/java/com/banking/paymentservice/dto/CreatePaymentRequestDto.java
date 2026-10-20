package com.banking.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequestDto {

    @NotBlank(message = "Missing account number")
    private String accountNumber;

    @NotBlank(message = "Amount is missing")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
