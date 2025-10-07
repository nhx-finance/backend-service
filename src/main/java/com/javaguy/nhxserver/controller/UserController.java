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
import java.util.Map;
import com.javaguy.nhxserver.model.dto.UpdateUserRequest;
import com.javaguy.nhxserver.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import com.javaguy.nhxserver.model.dto.WalletRequestDto;
import com.javaguy.nhxserver.exception.ApiException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing user profiles and data")
public class UserController {

    private final UserService userService;
    private final AzureBlobStorageService azureBlobStorageService;
    private final HederaService hederaService;

    @Operation(summary = "Update user details", description = "Updates the details of a specific user. Requires user to be authenticated and authorized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = User.class))),
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
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        try {
            User updatedUser = userService.updateUser(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(null, HttpStatus.NOT_FOUND, "User Not Found", e.getMessage(), "/api/v1/users/" + userId));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(null, HttpStatus.CONFLICT, "Conflict", e.getMessage(), "/api/v1/users/" + userId));
        } catch (Exception e) {
            log.error("An unexpected error occurred during user update for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "/api/v1/users/" + userId));
        }
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
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

            String oldImageUrl = user.getProfileImageUrl();

            String newImageUrl = azureBlobStorageService.uploadImage(file);

            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                azureBlobStorageService.deleteImage(oldImageUrl);
            }

            userService.updateProfileImage(userId, newImageUrl);

            return ResponseEntity.ok(Map.of("message", "Profile image uploaded successfully", "imageUrl", newImageUrl));
        } catch (IOException e) {
            log.error("Error uploading profile image for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(null, HttpStatus.BAD_REQUEST, "Upload Failed", e.getMessage(), "/api/v1/users/" + userId + "/profile-image"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(null, HttpStatus.NOT_FOUND, "User Not Found", e.getMessage(), "/api/v1/users/" + userId + "/profile-image"));
        } catch (Exception e) {
            log.error("An unexpected error occurred during profile image upload for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "/api/v1/users/" + userId + "/profile-image"));
        }
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

    @PostMapping("/{userId}/wallet")
    @PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
    public ResponseEntity<?> manageWallet(@PathVariable Long userId, @Valid @RequestBody WalletRequestDto dto) {
        try {
            hederaService.manageWallet(userId, dto);
            return ResponseEntity.ok(Map.of("message", "Wallet management initiated successfully"));
        } catch (ApiException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ErrorResponse(null, e.getStatus(), "Wallet Error", e.getMessage(), "/api/v1/users/" + userId + "/wallet"));
        } catch (Exception e) {
            log.error("An unexpected error occurred during wallet management for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", "/api/v1/users/" + userId + "/wallet"));
        }
    }
}
