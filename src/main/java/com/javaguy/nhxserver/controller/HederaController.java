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
                        @Parameter(description = "Amount of tokens to transfer (in smallest units)") @RequestParam @Positive long amount) {

                log.info("Processing token transfer request for user {} to account {}",
                                userDetails.getId(), accountId);

                HederaTransactionResponse response = hederaService.transferTokens(
                                userDetails.getId(), accountId, amount);

                return ResponseEntity.ok(response);
        }

        /**
         * Sell tokens: frontend sends tokenId, amountToBurn (smallest units),
         * amountUsdcToSend (USDC smallest units), and recipient account
         */
        @PostMapping("/sell")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Sell token for USDC", description = "Burns the sold token and transfers USDC from treasury to the recipient", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Sell operation successful", content = @Content(schema = @Schema(implementation = HederaTransactionResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
                        @ApiResponse(responseCode = "401", description = "Not authorized"),
                        @ApiResponse(responseCode = "500", description = "Error during sell operation")
        })
        public ResponseEntity<HederaTransactionResponse> sellTokens(
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @Parameter(description = "Token ID being sold (e.g. 0.0.12345)") @RequestParam @NotBlank String tokenId,
                        @Parameter(description = "Amount to burn (in token smallest units)") @RequestParam @Positive long amountToBurn,
                        @Parameter(description = "Amount of USDC to send (in smallest USDC units)") @RequestParam @Positive long amountUsdcToSend,
                        @Parameter(description = "Recipient Hedera account ID for USDC") @RequestParam @NotBlank String accountId) {

                log.info("Received sell request from user {}: token={}, burn={}, usdc={}, recipient={}",
                                userDetails.getId(), tokenId, amountToBurn, amountUsdcToSend, accountId);

                HederaTransactionResponse resp = hederaService.sellTokens(
                                userDetails.getId(), tokenId, amountToBurn, accountId, amountUsdcToSend);

                return ResponseEntity.ok(resp);
        }
}