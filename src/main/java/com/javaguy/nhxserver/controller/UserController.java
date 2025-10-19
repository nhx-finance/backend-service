package com.javaguy.nhxserver.controller;

import com.javaguy.nhxserver.exception.ResourceNotFoundException;
import com.javaguy.nhxserver.exception.handler.ErrorResponse;
import com.javaguy.nhxserver.model.entity.User;
import com.javaguy.nhxserver.service.azure.AzureBlobStorageService;
import com.javaguy.nhxserver.service.hedera.HederaService;
import com.javaguy.nhxserver.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import com.javaguy.nhxserver.model.dto.UpdateUserRequest;
import com.javaguy.nhxserver.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import com.javaguy.nhxserver.model.dto.WalletRequestDto;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.model.dto.RegisterRequest;
import com.javaguy.nhxserver.model.dto.RegistrationResponse;
import com.javaguy.nhxserver.service.security.AuthService;
import com.javaguy.nhxserver.model.dto.UserResponse;
import com.javaguy.nhxserver.model.dto.MessageResponse;
import com.javaguy.nhxserver.model.dto.WalletResponse;
import com.javaguy.nhxserver.model.dto.KycSubmissionDto;
import com.javaguy.nhxserver.model.dto.AssetDto;
import com.javaguy.nhxserver.model.dto.PortfolioSnapshotDto;
import com.javaguy.nhxserver.model.dto.TransactionDto;
import com.javaguy.nhxserver.model.dto.ProductAccessDto;
import com.javaguy.nhxserver.model.dto.PaymentMethodDto;
import com.javaguy.nhxserver.model.dto.AddPaymentMethodRequest;
import com.javaguy.nhxserver.model.dto.PaymentTransactionDto;
import com.javaguy.nhxserver.service.PortfolioSnapshotService;
import com.javaguy.nhxserver.service.TransactionService;
import com.javaguy.nhxserver.service.ProductAccessService;
import com.javaguy.nhxserver.service.PaymentMethodService;
import com.javaguy.nhxserver.service.PaymentTransactionService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import com.javaguy.nhxserver.model.entity.Transaction;
import com.javaguy.nhxserver.model.entity.PaymentMethod;
import com.javaguy.nhxserver.model.entity.PaymentTransaction;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing user profiles and data")
public class UserController {

    private final UserService userService;
    private final AzureBlobStorageService azureBlobStorageService;
    private final HederaService hederaService;
    private final AuthService authService;
    private final PortfolioSnapshotService portfolioSnapshotService;
    private final TransactionService transactionService;
    private final ProductAccessService productAccessService;
    private final PaymentMethodService paymentMethodService;
    private final PaymentTransactionService paymentTransactionService;

    @Operation(summary = "Get user profile by ID", description = "Retrieves the complete profile for a given user ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized to access this profile",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        List<AssetDto> assets = userService.getAssetsByUserId(userId).stream()
                .map(AssetDto::fromEntity)
                .collect(Collectors.toList());
        List<TransactionDto> transactions = transactionService.getTransactions(userId, null, null, null).stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());
        List<ProductAccessDto> productAccesses = productAccessService.getProductAccessByUserId(userId).stream()
                .map(ProductAccessDto::fromEntity)
                .collect(Collectors.toList());
        List<PaymentMethodDto> paymentMethods = paymentMethodService.getPaymentMethodsByUserId(userId).stream()
                .map(PaymentMethodDto::fromEntity)
                .collect(Collectors.toList());
        List<PaymentTransactionDto> paymentTransactions = paymentTransactionService.getPaymentTransactions(userId, null, null, null).stream()
                .map(PaymentTransactionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(UserResponse.fromUser(user, assets, transactions, productAccesses, paymentMethods, paymentTransactions));
    }

    @Operation(summary = "Update user details", description = "Updates the details of a specific user. Requires user to be authenticated and authorized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or bad request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not authorized to update this profile",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username, email, or phone number already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<MessageResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
            MessageResponse response = userService.updateUser(userId, request);
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "Upload user profile image", description = "Uploads a profile image for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or upload failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long userId, @RequestParam("image") MultipartFile file) throws IOException {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        String oldImageUrl = user.getProfileImageUrl();

        String newImageUrl = azureBlobStorageService.uploadImage(userId, file);

        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            azureBlobStorageService.deleteImage(oldImageUrl);
        } else {
            log.warn("No existing profile image for user {}. Skipping deletion.", userId);
        }

        userService.updateProfileImage(userId, newImageUrl);

        return ResponseEntity.ok(Map.of("profileImageUrl", newImageUrl, "uploadedAt", user.getUpdatedAt()));
    }

    @Operation(summary = "Get user profile image URL", description = "Retrieves the public URL of a user's profile image.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image URL retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "User or profile image not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<?> getProfileImage(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Profile image not found for user " + userId));
        }

        String imageUrl = user.getProfileImageUrl();

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "Set user wallet address", description = "Sets the blockchain wallet address for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet address set successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid wallet address format",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Wallet address already set",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{userId}/wallet")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<MessageResponse> setWalletAddress(@PathVariable Long userId, @Valid @RequestBody WalletRequestDto request) {
            MessageResponse response = userService.setWalletAddress(userId, request.walletAddress());
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user wallet address", description = "Retrieves the blockchain wallet address for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet address retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or wallet address not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/wallet")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<WalletResponse> getWalletAddress(@PathVariable Long userId) {
            WalletResponse walletResponse = userService.getWalletAddress(userId);
            return ResponseEntity.ok(walletResponse);
    }

    @Operation(summary = "Submit KYC information", description = "Submits KYC (Know Your Customer) information for a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC submitted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or bad request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{userId}/kyc")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<MessageResponse> submitKyc(@PathVariable Long userId, @Valid @RequestBody KycSubmissionDto kycDto) {
            MessageResponse response = userService.submitKyc(userId, kycDto);
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user portfolio history", description = "Retrieves the historical portfolio snapshots for a user within a given date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio history retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PortfolioSnapshotDto.class, type = "array"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/portfolio/history")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<List<PortfolioSnapshotDto>> getPortfolioHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
            List<PortfolioSnapshot> snapshots = portfolioSnapshotService.getPortfolioHistory(userId, startDate, endDate);
            List<PortfolioSnapshotDto> dtos = snapshots.stream()
                    .map(PortfolioSnapshotDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get user transactions", description = "Retrieves a list of transactions for a user, with optional filtering by type and date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionDto.class, type = "array"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/transactions")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

            List<Transaction> transactions = transactionService.getTransactions(userId, type, startDate, endDate);
            List<TransactionDto> dtos = transactions.stream()
                    .map(TransactionDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);

    }

    @Operation(summary = "Add a new payment method", description = "Adds a new payment method for the specified user. For M-Pesa, also updates the user's phone number if it's empty.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment method added successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentMethodDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or bad request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Payment method already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{userId}/payment-methods")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<PaymentMethodDto> addPaymentMethod(@PathVariable Long userId, @Valid @RequestBody AddPaymentMethodRequest request) {
            PaymentMethod paymentMethod = paymentMethodService.addPaymentMethod(userId, request.name(), request.mobileNumber());
            return new ResponseEntity<>(PaymentMethodDto.fromEntity(paymentMethod), HttpStatus.CREATED);
    }

    @Operation(summary = "Get user payment methods", description = "Retrieves a list of payment methods for the specified user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment methods retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentMethodDto.class, type = "array"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/payment-methods")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<List<PaymentMethodDto>> getPaymentMethods(@PathVariable Long userId) {
            List<PaymentMethod> paymentMethods = paymentMethodService.getPaymentMethodsByUserId(userId);
            List<PaymentMethodDto> dtos = paymentMethods.stream()
                    .map(PaymentMethodDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get user payment transactions", description = "Retrieves a list of payment transactions for a user, with optional filtering by type and date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment transactions retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentTransactionDto.class, type = "array"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/payment-transactions")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.userId")
    public ResponseEntity<List<PaymentTransactionDto>> getPaymentTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
            List<PaymentTransaction> paymentTransactions = paymentTransactionService.getPaymentTransactions(userId, type, startDate, endDate);
            List<PaymentTransactionDto> dtos = paymentTransactions.stream()
                    .map(PaymentTransactionDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
    }
}
