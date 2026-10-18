package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.dto.TransactionResponse;
import com.banking.frauddetectionservice.fraudCheck.FraudCheckResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
public class FraudCheck {

    /**
     * Performs fraud checks on a transaction using velocity, average amount, and balance percentage rules.
     * @param accountNumber The account being analyzed.
     * @param allTransactions List of recent transaction amounts for this account.
     * @param currentTransactionAmount The amount of the current transaction.
     * @param accountBalance The current balance of the account.
     * @return FraudCheckResult containing whether fraud is detected and the reason.
     */
    public FraudCheckResult performFraudChecks( String accountNumber, List<TransactionResponse> allTransactions, BigDecimal currentTransactionAmount, BigDecimal accountBalance) {

        // Start with a default result: no fraud detected
        FraudCheckResult result = new FraudCheckResult(false, "No fraud detected");

        // 🕒 1️⃣ Velocity Check — count how many transactions happened in the last minute
        LocalDateTime now = LocalDateTime.now();

        int countInLastMinute = 0;

        for (TransactionResponse tx : allTransactions) {
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isAfter(now.minusMinutes(2))) {
                countInLastMinute++;
            }
        }

        if (countInLastMinute > 10) {
            result.setFraud(true);
            result.setReason("Velocity check failed: more than 10 transactions in the last minute");
            return result;
        }else{
            log.info("Velocity check passed for all recent transaction");
        }

        // 💰 2️⃣ Transaction Average Check — calculate average transaction amount
        BigDecimal total = BigDecimal.ZERO;

        int count = 0;

        for (TransactionResponse tx : allTransactions) {
            if (tx.getAmount() != null) {
                total = total.add(tx.getAmount());
                count++;
            }
        }

        BigDecimal average = BigDecimal.ZERO;
        if (count > 0) {
            average = total.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
        }

        if (average.compareTo(BigDecimal.ZERO) > 0 && currentTransactionAmount.compareTo(average.multiply(BigDecimal.valueOf(3))) > 0) {
            result.setFraud(true);
            result.setReason("Transaction average check failed: current amount exceeds 3× average");
            return result;
        }else {
            log.info("Average transaction check passed");
        }

        // 📉 3️⃣ Balance Percentage Check — check if transaction drains too much of the balance
        if (accountBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentageTransferred = currentTransactionAmount
            .divide(accountBalance, 2, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

            if (percentageTransferred.compareTo(BigDecimal.valueOf(90)) > 0) {
                result.setFraud(true);
                result.setReason("Balance percentage check failed: transferring over 90% of balance");
                return result;
            }else {
                log.info("Balance percentage check passed");
            }
        }

        // ✅ If none of the checks triggered, return safe result
        return result;
    }

}
