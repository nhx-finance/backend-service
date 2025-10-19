package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.repository.TransactionRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<Transaction> getTransactions(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate) {
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
            return transactionRepository.findByUserUserIdAndTypeAndDateBetween(userId, type, startDate, endDate);
        } else {
            return transactionRepository.findByUserUserIdAndDateBetween(userId, startDate, endDate);
        }
    }
}
