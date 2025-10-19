package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserUserIdAndTypeAndDateBetween(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate);
    List<Transaction> findByUserUserIdAndDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
