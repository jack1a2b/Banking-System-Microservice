package com.banking.paymentservice.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
public enum PaymentStatus {
    CREATED,
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}