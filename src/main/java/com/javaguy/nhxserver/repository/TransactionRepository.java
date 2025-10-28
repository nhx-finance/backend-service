package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transactions by user ID, ordered by creation date
     */
    List<Transaction> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find transactions by user ID and status
     */
    List<Transaction> findByUserUserIdAndStatus(Long userId, TransactionStatus status);

    /**
     * Find transactions by user ID within date range
     */
    List<Transaction> findByUserUserIdAndCreatedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find transaction by Hedera transaction ID
     */
    Optional<Transaction> findByHederaTransactionId(String hederaTransactionId);

    /**
     * Find all transactions with specific status
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Find stuck transactions (created before certain time with pending status)
     */
    @Query("SELECT t FROM Transaction t WHERE t.status IN :statuses AND t.createdAt < :cutoffTime")
    List<Transaction> findStuckTransactions(
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Count transactions by user and status
     */
    long countByUserUserIdAndStatus(Long userId, TransactionStatus status);

    /**
     * Get total transaction volume for user
     */
    @Query("SELECT COALESCE(SUM(t.amountUsdc), 0) FROM Transaction t WHERE t.user.userId = :userId AND t.status = :status")
    java.math.BigDecimal getTotalVolumeByUser(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status
    );
}
