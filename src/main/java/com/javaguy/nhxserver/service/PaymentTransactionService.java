package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.PaymentTransaction;
import com.javaguy.nhxserver.repository.PaymentTransactionRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    public List<PaymentTransaction> getPaymentTransactions(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (startDate == null) {
            startDate = LocalDateTime.MIN;
        }
        if (endDate == null) {
            endDate = LocalDateTime.MAX;
        }

        if (startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }

        if (type != null && !type.isEmpty()) {
            return paymentTransactionRepository.findByUserUserIdAndTypeAndDateBetween(userId, type, startDate, endDate);
        } else {
            return paymentTransactionRepository.findByUserUserIdAndDateBetween(userId, startDate, endDate);
        }
    }
}
