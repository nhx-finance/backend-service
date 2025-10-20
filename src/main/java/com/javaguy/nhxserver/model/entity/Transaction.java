package com.javaguy.nhxserver.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trading transaction entity representing buy/sell operations for assets (e.g., stocks).
 * Not for fiat top-ups or withdrawals — see PaymentTransaction for those.
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId; // public-facing ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String stock;
    private BigDecimal amount;
    private BigDecimal quantity;
    private String type; // buy or sell
    private LocalDateTime date;

    @PrePersist
    protected void onCreate() {
        date = LocalDateTime.now();
    }
}
