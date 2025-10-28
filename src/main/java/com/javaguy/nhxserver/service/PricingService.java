package com.javaguy.nhxserver.service;

import com.javaguy.nhxserver.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@RequiredArgsConstructor
public class PricingService {

    @Value("${exchange.rate-source:fixed}")
    private String rateSource;

    @Value("${exchange.default-rate:1000}")
    private BigDecimal defaultRate;

    @Value("${exchange.fee-percentage:2.0}")
    private BigDecimal feePercentage;

    private static final int SCALE = 8; // For token amounts and exchange rates

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // For MVP, return a fixed rate. In a real app, this would call an external API.
        if ("KES".equalsIgnoreCase(fromCurrency) && "TOKEN".equalsIgnoreCase(toCurrency)) {
            return defaultRate;
        } else if ("TOKEN".equalsIgnoreCase(fromCurrency) && "KES".equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE.divide(defaultRate, SCALE, RoundingMode.HALF_UP);
        } else {
            log.warn("Unsupported exchange rate request: {} to {}. Using default rate.", fromCurrency, toCurrency);
            return defaultRate; // Fallback
        }
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feePercentage.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP));
    }

    public BigDecimal applyFee(BigDecimal amount) {
        return amount.subtract(calculateFee(amount));
    }

    public BigDecimal calculateTokensForKes(BigDecimal amountKes) {
        BigDecimal rate = getExchangeRate("KES", "TOKEN");
        return amountKes.divide(rate, SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateKesForTokens(BigDecimal tokenAmount) {
        BigDecimal rate = getExchangeRate("TOKEN", "KES");
        return tokenAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
