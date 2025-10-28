package com.javaguy.nhxserver.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio entity - tracks user token holdings
 */
@Entity
@Table(name = "portfolios", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token_id"}),
       indexes = {
               @Index(name = "idx_user_id", columnList = "user_id"),
               @Index(name = "idx_token_id", columnList = "token_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_id", nullable = false)
    private String tokenId; // Hedera Token ID

    @Column(name = "balance", precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    public void prePersist() {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }
}
