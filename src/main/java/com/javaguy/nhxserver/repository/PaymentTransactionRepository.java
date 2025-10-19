package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUserUserIdAndTypeAndDateBetween(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate);
    List<PaymentTransaction> findByUserUserIdAndDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
}
