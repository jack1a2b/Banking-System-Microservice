package com.banking.frauddetectionservice.dto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionEvent {
    private String accountNumber;
    private BigDecimal amount;
}
