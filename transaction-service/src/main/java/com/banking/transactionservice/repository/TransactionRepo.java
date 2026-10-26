package com.banking.transactionservice.repository;

import com.banking.transactionservice.model.Transaction;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, String> {

    @Query(value = "SELECT * FROM transactions", nativeQuery = true)
    List<Transaction> findAllTransations();

    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(@Valid String accountNumber);
}
