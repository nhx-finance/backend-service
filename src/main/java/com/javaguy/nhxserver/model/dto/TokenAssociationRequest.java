package com.javaguy.nhxserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token association
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAssociationRequest {
    private String accountId;
    private String accountPrivateKey;
    private boolean associateBothTokens; // If true, associate both USDC and nhSAF
}
