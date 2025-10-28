package com.javaguy.nhxserver.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio Snapshot entity - historical portfolio values
 */
@Entity
@Table(name = "portfolio_snapshots", indexes = {
        @Index(name = "idx_user_snapshot_time", columnList = "user_id,snapshot_time"),
        @Index(name = "idx_snapshot_time", columnList = "snapshot_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;

    @Column(name = "total_value_usdc", precision = 19, scale = 6)
    private BigDecimal totalValueUsdc;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "holdings_json", columnDefinition = "TEXT")
    private String holdingsJson; // JSON representation of all holdings

    @PrePersist
    public void prePersist() {
        // if (id == null) { // Removed manual ID generation
        //     id = UUID.randomUUID();
        // }
    }
}
