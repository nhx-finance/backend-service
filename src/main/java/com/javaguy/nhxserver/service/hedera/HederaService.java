package com.javaguy.nhxserver.service.hedera;

import com.hedera.hashgraph.sdk.*;
import com.javaguy.nhxserver.config.HederaConfig;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.HederaTransactionResponse;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.enums.TransactionStatus;
import com.javaguy.nhxserver.model.enums.TransactionType;
import com.javaguy.nhxserver.repository.TransactionRepository;
import com.javaguy.nhxserver.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import com.javaguy.nhxserver.model.dto.SellRequestDto;

@Service
@Slf4j
public class HederaService {
    private final Client client;
    private static final TokenId KCB_TOKEN = TokenId.fromString("0.0.7142699");
    private static final TokenId KQ_TOKEN = TokenId.fromString("0.0.7142834");
    private static final TokenId KEGN_TOKEN = TokenId.fromString("0.0.7142885");
    private static final TokenId HAFR_TOKEN = TokenId.fromString("0.0.7142913");
    private static final TokenId EQTY_TOKEN = TokenId.fromString("0.0.7142958");
    private static final TokenId usdcTokenId = TokenId.fromString("0.0.7135358");
    private static final TokenId SCOM_TOKEN = TokenId.fromString("0.0.7135370");

    private final AccountId treasuryAccountId;
    private final PrivateKey treasuryPrivateKey;
    private final TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;

    private static final int TOKEN_DECIMALS = 6;
    private static final int USDC_DECIMALS = 6;
    private static final long MAX_TRANSACTION_FEE_HBAR = 1;

    public HederaService(Client client,
            HederaConfig hederaConfig,
            TransactionRepository transactionRepository,
            PortfolioService portfolioService) {
        this.client = client;
        this.treasuryAccountId = AccountId.fromString(hederaConfig.getTreasuryAccountId());
        this.treasuryPrivateKey = PrivateKey.fromStringECDSA(hederaConfig.getTreasuryKey());
        this.transactionRepository = transactionRepository;
        this.portfolioService = portfolioService;

        log.info("HederaService initialized with treasury account: {}",
                treasuryAccountId);
    }

    /**
     * Transfer tokens from treasury to a user's account and update their portfolio
     * 
     * @param userId                Internal user ID for portfolio tracking
     * @param recipientAccountIdStr Hedera account ID to receive tokens
     * @param tokenAmount           Amount of tokens to transfer
     * @return Transaction details including Hedera transaction ID
     */
    @Transactional
    public HederaTransactionResponse transferTokens(Long userId, String tokenSymbol, String recipientAccountIdStr, long tokenAmount) {
        log.info("Transferring {} tokens to account {}", tokenAmount, recipientAccountIdStr);

        if (!isValidAccountId(recipientAccountIdStr)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid account ID format");
        }
        AccountId recipientAccountId = AccountId.fromString(recipientAccountIdStr);
        TokenId tokenId = getTokenIdBySymbol(tokenSymbol);
        try {
            // Create the transfer transaction
            TransferTransaction transaction = new TransferTransaction()
                    .addTokenTransfer(tokenId, treasuryAccountId, -tokenAmount)
                    .addTokenTransfer(tokenId, recipientAccountId, tokenAmount)
                    .setMaxTransactionFee(new Hbar(MAX_TRANSACTION_FEE_HBAR))
                    .freezeWith(client);

            // Sign and execute
            TransactionResponse txResponse = transaction
                    .sign(treasuryPrivateKey)
                    .execute(client);

            // Get the receipt
            TransactionReceipt receipt = txResponse.getReceipt(client);

            if (receipt.status == Status.SUCCESS) {
                String transactionId = txResponse.transactionId.toString();
                log.info("Successfully transferred {} tokens to account {}, txn: {}",
                        tokenAmount, recipientAccountId, transactionId);

                // Convert token amount to decimal format
                BigDecimal tokenDecimalAmount = BigDecimal.valueOf(tokenAmount)
                        .movePointLeft(TOKEN_DECIMALS);

                // Update user's portfolio
                portfolioService.updatePortfolio(
                        userId,
                        tokenId.toString(),
                        tokenDecimalAmount,
                        TransactionType.TOKEN_TRANSFER);

                // Record the transaction
                Transaction tx = Transaction.builder()
                        .type(TransactionType.TOKEN_TRANSFER)
                        .status(TransactionStatus.COMPLETED)
                        .tokenAmount(tokenDecimalAmount)
                        .hederaAccountId(recipientAccountIdStr)
                        .hederaTransactionId(transactionId)
                        .memo("Token transfer from treasury")
                        .completedAt(LocalDateTime.now())
                        .build();

                transactionRepository.save(tx);

                return HederaTransactionResponse.builder()
                        .success(true)
                        .transactionId(transactionId)
                        .status(receipt.status.toString())
                        .message(String.format("Successfully transferred %s tokens", tokenDecimalAmount))
                        .build();

            } else {
                log.error("Token transfer failed with status: {}", receipt.status);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "TransferFailed",
                        "Transaction failed with status: " + receipt.status);
            }

        } catch (Exception e) {
            handleTransactionException(e);
            return null;
        }
    }

    /**
     * Sell tokens: burn the incoming token and send USDC from the treasury to the
     * recipient.
     *
     * The frontend must send the tokenId (the token being sold), the amount to burn
     * (in
     * smallest units) and the amount of USDC to send (in USDC smallest units,
     * typically 6 decimals),
     * and the recipient Hedera account id that should receive the USDC.
     */
    @Transactional
    public HederaTransactionResponse sellTokens(Long userId, SellRequestDto request) {
        log.info("Sell request: user={}, tokenSymbol={}, burnAmount={}, usdcAmount={}, recipient={}",
                userId, request.tokenSymbol(), request.amountToBurn(), request.amountUsdcToSend(), request.recipientAccountIdStr());

        if (!isValidAccountId(request.recipientAccountIdStr())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid recipient account ID format");
        }

        long amountToBurn;
        long amountUsdcToSend;
        try {
            amountToBurn = Long.parseLong(request.amountToBurn());
            amountUsdcToSend = Long.parseLong(request.amountUsdcToSend());
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid number format for amount: " + e.getMessage());
        }

        if (amountToBurn <= 0 || amountUsdcToSend <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Amounts must be positive.");
        }

        TokenId tokenId;
        try {
            tokenId = getTokenIdBySymbol(request.tokenSymbol());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "InvalidInput", "Invalid tokenId format");
        }

        AccountId recipientAccountId = AccountId.fromString(request.recipientAccountIdStr());

        try {
            // Burn the sold tokens
            TokenBurnTransaction burnTx = new TokenBurnTransaction()
                    .setTokenId(tokenId)
                    .setAmount(amountToBurn)
                    .freezeWith(client);

            TransactionResponse burnResponse = burnTx
                    .sign(treasuryPrivateKey)
                    .execute(client);

            TransactionReceipt burnReceipt = burnResponse.getReceipt(client);

            if (burnReceipt.status != Status.SUCCESS) {
                log.error("Token burn failed for token {} status={}", tokenId, burnReceipt.status);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "BurnFailed",
                        "Token burn failed: " + burnReceipt.status);
            }

            log.info("Token burn succeeded: token={}, amount={}, burnTxn={}",
                    tokenId, amountToBurn, burnResponse.transactionId);

            // Transfer USDC from treasury to recipient
            TransferTransaction usdcTransfer = new TransferTransaction()
                    .addTokenTransfer(usdcTokenId, treasuryAccountId, -amountUsdcToSend)
                    .addTokenTransfer(usdcTokenId, recipientAccountId, amountUsdcToSend)
                    .setMaxTransactionFee(new Hbar(MAX_TRANSACTION_FEE_HBAR))
                    .freezeWith(client);

            TransactionResponse usdcTxResp = usdcTransfer
                    .sign(treasuryPrivateKey)
                    .execute(client);

            TransactionReceipt usdcReceipt = usdcTxResp.getReceipt(client);

            if (usdcReceipt.status != Status.SUCCESS) {
                log.error("USDC transfer failed: status={}", usdcReceipt.status);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "UsdcTransferFailed",
                        "USDC transfer failed: " + usdcReceipt.status);
            }

            log.info("USDC transfer succeeded to {} amount={} txn={}", recipientAccountId, amountUsdcToSend,
                    usdcTxResp.transactionId);

            // Convert amounts to decimals for portfolio/recording
            BigDecimal tokenDecimalAmount = BigDecimal.valueOf(amountToBurn).movePointLeft(TOKEN_DECIMALS);
            BigDecimal usdcDecimalAmount = BigDecimal.valueOf(amountUsdcToSend).movePointLeft(USDC_DECIMALS);

            // Update user's portfolio (subtract sold tokens)
            portfolioService.updatePortfolio(userId, tokenId.toString(), tokenDecimalAmount, TransactionType.SALE);

            // Record transaction (store USDC transfer tx id as primary)
            Transaction tx = Transaction.builder()
                    .type(TransactionType.SALE)
                    .status(TransactionStatus.COMPLETED)
                    .amountUsdc(usdcDecimalAmount)
                    .tokenAmount(tokenDecimalAmount)
                    .hederaAccountId(request.recipientAccountIdStr())
                    .hederaTransactionId(usdcTxResp.transactionId.toString())
                    .memo(String.format("Sold %s tokens for %s USDC", tokenDecimalAmount, usdcDecimalAmount))
                    .completedAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(tx);

            return HederaTransactionResponse.builder()
                    .success(true)
                    .transactionId(usdcTxResp.transactionId.toString())
                    .status(usdcReceipt.status.toString())
                    .message(String.format("Burned %s tokens and transferred %s USDC", tokenDecimalAmount,
                            usdcDecimalAmount))
                    .build();

        } catch (Exception e) {
            handleTransactionException(e);
            return null;
        }
    }

    /**
     * Handle transaction errors
     */
    private void handleTransactionException(Exception e) {
        log.error("Error during token transfer: {}", e.getMessage(), e);

        if (e instanceof PrecheckStatusException pre) {
            String message = switch (pre.status.toString()) {
                case "INSUFFICIENT_ACCOUNT_BALANCE" ->
                    "Insufficient balance to complete transaction";
                case "INVALID_ACCOUNT_ID" ->
                    "Invalid account ID provided";
                case "INSUFFICIENT_TOKEN_BALANCE" ->
                    "Insufficient token balance for this transaction";
                case "TOKEN_NOT_ASSOCIATED_TO_ACCOUNT" ->
                    "Token not associated with account. Please associate tokens first";
                default ->
                    "Transaction precheck failed: " + pre.getMessage();
            };
            throw new ApiException(HttpStatus.BAD_REQUEST, "PrecheckFailed", message);
        } else if (e instanceof ReceiptStatusException rec) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "ReceiptStatusError",
                    "Transaction failed during execution: " + rec.getMessage());
        } else if (e instanceof TimeoutException) {
            throw new ApiException(HttpStatus.REQUEST_TIMEOUT,
                    "HederaTimeout",
                    "Transaction timed out. Please try again.");
        } else if (e instanceof ApiException apiEx) {
            throw apiEx;
        } else {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "UnexpectedError",
                    "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Validate account ID format
     */
    private boolean isValidAccountId(String accountIdStr) {
        try {
            AccountId.fromString(accountIdStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the treasury account ID
     */
    public String getTreasuryAccountIdString() {
        return treasuryAccountId.toString();
    }


    /**
     * Query token balance for an account and token
     */
    public long getTokenBalance(AccountId accountId, TokenId tokenId) {
        try {
            AccountBalance balance = new AccountBalanceQuery()
                    .setAccountId(accountId)
                    .execute(client);

            Long val = balance.tokens.get(tokenId);
            return val == null ? 0L : val.longValue();
        } catch (Exception e) {
            log.error("Failed to fetch token balance for account {} token {}: {}", accountId, tokenId, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "BalanceQueryFailed",
                    "Failed to query token balance: " + e.getMessage());
        }
    }

    private TokenId getTokenIdBySymbol(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "SCOM" -> SCOM_TOKEN;
            case "KCB" -> KCB_TOKEN;
            case "KQ" -> KQ_TOKEN;
            case "KEGN" -> KEGN_TOKEN;
            case "HAFR" -> HAFR_TOKEN;
            case "EQTY" -> EQTY_TOKEN;
            default -> throw new IllegalArgumentException("Unsupported token symbol: " + symbol);
        };
    }
}