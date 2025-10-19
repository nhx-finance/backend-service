package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import com.javaguy.nhxserver.repository.PortfolioSnapshotRepository;
import com.javaguy.nhxserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final UserRepository userRepository;

    public List<PortfolioSnapshot> getPortfolioHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (startDate == null) {
            startDate = LocalDate.MIN;
        }
        if (endDate == null) {
            endDate = LocalDate.MAX;
        }

        if (startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }

        return portfolioSnapshotRepository.findByUserUserIdAndDateBetween(userId, startDate, endDate);
    }
}
