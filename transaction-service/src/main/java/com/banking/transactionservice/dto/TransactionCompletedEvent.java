package com.banking.transactionservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCompletedEvent {
    private String transactionid;
    private String senderAccountNumber;
    private String revieverAccountNumber;
    private BigDecimal amount;
    private String ReferenceNumber;

}
