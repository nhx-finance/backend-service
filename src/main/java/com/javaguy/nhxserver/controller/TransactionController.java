package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.TransactionDto;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.service.TransactionService;
import com.javaguy.nhxserver.service.user.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

        private final TransactionService transactionService;

        @GetMapping("/{transactionId}")
        public ResponseEntity<TransactionDto> getTransactionById(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @PathVariable Long transactionId) {
                log.info("User {} fetching transaction by ID: {}", userDetails.getEmail(), transactionId);

                Transaction transaction = transactionService.getTransaction(transactionId);
                // Ensure the transaction belongs to the authenticated user
                if (!transaction.getUser().getUserId().equals(userDetails.getId())) {
                        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
                return ResponseEntity.ok(TransactionDto.fromEntity(transaction));
        }

        @GetMapping
        public ResponseEntity<List<TransactionDto>> getUserTransactions(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @RequestParam(required = false) TransactionStatus status,
                        @RequestParam(required = false) LocalDateTime startDate,
                        @RequestParam(required = false) LocalDateTime endDate) {
                log.info("User {} fetching transactions with status {}, start date {}, end date {}",
                                userDetails.getEmail(), status, startDate, endDate);

                List<Transaction> transactions = transactionService.getUserTransactions(userDetails.getId(), status,
                                startDate, endDate);
                List<TransactionDto> transactionDtos = transactions.stream()
                                .map(TransactionDto::fromEntity)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(transactionDtos);
        }
}
