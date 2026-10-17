package com.banking.transactionservice.dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class TransactionEvent {
    private String accountNumber;
    private BigDecimal amount;
}
