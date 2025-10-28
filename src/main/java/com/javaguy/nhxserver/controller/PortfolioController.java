package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.PortfolioDto;
import com.javaguy.nhxserver.model.dto.PortfolioPerformanceDto;
import com.javaguy.nhxserver.model.dto.PortfolioSnapshotDto;
import com.javaguy.nhxserver.service.PortfolioService;
import com.javaguy.nhxserver.service.user.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioDto>> getCurrentPortfolio(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} fetching current portfolio.", userDetails.getEmail());
        List<PortfolioDto> portfolio = portfolioService.getCurrentPortfolio(userDetails.getId()).stream()
                .map(PortfolioDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PortfolioSnapshotDto>> getPortfolioHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("User {} fetching portfolio history from {} to {}", userDetails.getEmail(), startDate, endDate);
        List<PortfolioSnapshotDto> history = portfolioService.getPortfolioHistory(userDetails.getId(), 
                (startDate != null ? startDate.atStartOfDay() : null), // Convert startDate
                (endDate != null ? endDate.atStartOfDay().withHour(23).withMinute(59).withSecond(59) : null)) // Convert endDate
                .stream()
                .map(PortfolioSnapshotDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/performance")
    public ResponseEntity<PortfolioPerformanceDto> getPortfolioPerformance(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.info("User {} fetching portfolio performance.", userDetails.getEmail());

        PortfolioPerformanceDto performance = portfolioService.calculatePerformance(userDetails.getId());

        return ResponseEntity.ok(performance);
    }
}
