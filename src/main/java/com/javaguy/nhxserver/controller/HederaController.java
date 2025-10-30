package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.model.dto.HederaTransactionResponse;
import com.javaguy.nhxserver.service.hedera.HederaService;
import com.javaguy.nhxserver.service.user.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import com.javaguy.nhxserver.model.dto.SellRequestDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/hedera")
@Tag(name = "Hedera Token Service", description = "APIs for token transfers on Hedera")
@RequiredArgsConstructor
@Validated
@Slf4j
public class HederaController {

        private final HederaService hederaService;

        /**
         * Transfer tokens from treasury to user's Hedera account
         */
        @PostMapping("/transfer")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Transfer tokens to user's Hedera account", description = "Transfers specified amount of tokens from the treasury account to the user's Hedera account", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Token transfer successful", content = @Content(schema = @Schema(implementation = HederaTransactionResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                        @ApiResponse(responseCode = "401", description = "Not authorized to perform transfer"),
                        @ApiResponse(responseCode = "500", description = "Error during token transfer")
        })
        public ResponseEntity<HederaTransactionResponse> transferTokens(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "User's Hedera account ID (0.0.123456)") @RequestParam @NotBlank String accountId,
                        @Parameter(description = "Token Symbol") @RequestParam @NotBlank String tokenSymbol,
                        @Parameter(description = "Amount of tokens to transfer (in smallest units)") @RequestParam @Positive long amount) {

                log.info("Processing token transfer request for user {} to account {}",
                                userDetails.getId(), accountId);

                HederaTransactionResponse response = hederaService.transferTokens(
                                userDetails.getId(), tokenSymbol, accountId, amount);

                return ResponseEntity.ok(response);
        }

      
        @PostMapping("/sell")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Sell tokens for USDC", description = "Allows a user to sell a specified amount of a token by burning it on Hedera and receiving USDC in return. All details are provided in the request body.", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Sell operation successful", content = @Content(schema = @Schema(implementation = HederaTransactionResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input parameters (e.g., invalid token symbol, amounts, or recipient account ID)"),
                        @ApiResponse(responseCode = "401", description = "Not authorized to perform the sell operation"),
                        @ApiResponse(responseCode = "500", description = "Error during sell operation or Hedera network interaction")
        })
        public ResponseEntity<HederaTransactionResponse> sellTokens(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Valid @RequestBody SellRequestDto request) {

                log.info("Received sell request from user {}: token={}, burn={}, usdc={}, recipient={}",
                                userDetails.getId(), request.tokenSymbol(), request.amountToBurn(), request.amountUsdcToSend(), request.recipientAccountIdStr());

                HederaTransactionResponse resp = hederaService.sellTokens(
                                userDetails.getId(), request);

                return ResponseEntity.ok(resp);
        }
}