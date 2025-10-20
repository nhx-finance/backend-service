package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.PaymentTransaction;
import com.javaguy.nhxserver.repository.PaymentTransactionRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPaymentTransactions(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        LocalDateTime safeStart = (startDate != null) ? startDate : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime safeEnd = (endDate != null) ? endDate : LocalDateTime.of(3000, 1, 1, 0, 0);

        if (safeStart.isAfter(safeEnd)) {
            return Collections.emptyList();
        }

        if (type != null && !type.isBlank()) {
            return paymentTransactionRepository.findByUserUserIdAndTypeAndDateBetween(userId, type, safeStart, safeEnd);
        } else {
            return paymentTransactionRepository.findByUserUserIdAndDateBetween(userId, safeStart, safeEnd);
        }
    }
}
