package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.dto.TransactionResponse;
import com.banking.frauddetectionservice.dto.FraudCheckResult;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
public class FraudCheck {

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

        double maximum_transaction_per_minut = 10;
        if (countInLastMinute > maximum_transaction_per_minut) {
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

        double fraud_amount_multiplier = 3;
        if (average.compareTo(BigDecimal.ZERO) > 0 && currentTransactionAmount.compareTo(average.multiply(BigDecimal.valueOf(fraud_amount_multiplier))) > 0) {
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
