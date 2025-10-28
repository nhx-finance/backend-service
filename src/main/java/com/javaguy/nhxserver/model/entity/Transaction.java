package com.javaguy.nhxserver.model.entity;

import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.model.enums.TransactionType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity - represents buy/sell operations
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_user_status_created", columnList = "user_id,status,created_at"),
        @Index(name = "idx_hedera_tx_id", columnList = "hedera_transaction_id"),
        @Index(name = "idx_user_created", columnList = "user_id,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "amount_usdc", precision = 19, scale = 6)
    private BigDecimal amountUsdc;

    @Column(name = "token_amount", precision = 19, scale = 2)
    private BigDecimal tokenAmount;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "hedera_account_id")
    private String hederaAccountId;

    @Column(name = "hedera_transaction_id", unique = true)
    private String hederaTransactionId;

    @Column(name = "fees", precision = 19, scale = 6)
    private BigDecimal fees;

    @Column(name = "memo", length = 500)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // @PrePersist // Removed manual ID generation
    // public void prePersist() {
    //     if (id == null) {
    //         id = UUID.randomUUID();
    //     }
    // }
}
