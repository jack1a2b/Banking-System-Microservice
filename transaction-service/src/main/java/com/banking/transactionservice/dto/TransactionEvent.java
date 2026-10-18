package com.banking.transactionservice.dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class TransactionEvent {
    private String transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private String reason = "";
    private boolean isFraud = false;
}
