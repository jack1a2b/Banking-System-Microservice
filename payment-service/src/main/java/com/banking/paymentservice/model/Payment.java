package com.banking.paymentservice.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments")
@RequiredArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "missing razorpay order id")
    private String razorpayOrderId;

    @NotBlank(message = "missing razorpay payment id")
    private String razorpayPaymentId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, precision = 15,scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String description;

    private String failureReason;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updateAt;

}
