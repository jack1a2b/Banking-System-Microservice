package com.banking.paymentservice.repository;

import com.banking.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query(value = "SELECT * FROM payments WHERE razorpayOrderId = :orderId order by createdAt DESC ", nativeQuery = true)
    Payment fineByRazorpayOrId(String orderId);
}
