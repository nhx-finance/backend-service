package com.javaguy.nhxserver.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment transaction entity for fiat movements (deposits/withdrawals), e.g., Mpesa top-ups.
 * Distinct from Transaction, which captures trading buys/sells of assets.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId; // public-facing ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String type; // deposit or withdrawal
    private String method; // "Mpesa"
    private BigDecimal amount;
    private String currency; // "KES"
    //private String status; // pending, completed, failed
    private LocalDateTime date;
    private String description;

    @PrePersist
    protected void onCreate() {
        date = LocalDateTime.now();
    }
}
