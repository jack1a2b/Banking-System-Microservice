package com.banking.frauddetectionservice.model;



public enum TransactionStatus {

    /**
     * Transaction lifecycle flow:
     * PENDING -> PROCESSING -> COMPLETED(clean transaction)
     */
    PENDING,
    VERIFICATION,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED,
    PROCESSING
}
