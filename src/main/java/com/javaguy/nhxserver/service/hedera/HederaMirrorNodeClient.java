package com.javaguy.nhxserver.service.hedera;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaguy.nhxserver.exception.ApiException;
import com.javaguy.nhxserver.utils.HederaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class HederaMirrorNodeClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hedera.mirror-node-url:" + HederaConstants.MIRROR_NODE_BASE_URL_TESTNET + "}")
    private String mirrorNodeUrl;

    /**
     * Queries the Hedera Mirror Node for crypto transfers to a specific account.
     *
     * @param accountId The account ID to filter transactions by (e.g., treasury account).
     * @param tokenId   (Optional) The token ID to filter transfers by.
     * @param limit     (Optional) The maximum number of transactions to retrieve.
     * @param order     (Optional) The order of results (asc/desc).
     * @param timestamp (Optional) The timestamp to query transactions after
     * @return A list of JsonNode representing the transactions.
     */
    public List<JsonNode> getCryptoTransfersToAccount(String accountId, String tokenId, Integer limit, String order, String timestamp) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(mirrorNodeUrl)
                .path(HederaConstants.MIRROR_NODE_ACCOUNT_TRANSACTIONS_PATH)
                .queryParam("account.id", accountId)
                .queryParam("transactiontype", "CRYPTOTRANSFER");

        if (tokenId != null && !tokenId.isBlank()) {
            uriBuilder.queryParam("tokenid", tokenId);
        }
        if (limit != null) {
            uriBuilder.queryParam("limit", limit);
        }
        if (order != null && !order.isBlank()) {
            uriBuilder.queryParam("order", order);
        }
        if (timestamp != null && !timestamp.isBlank()) {
            uriBuilder.queryParam("timestamp", timestamp);
        }

        String url = uriBuilder.toUriString();
        log.debug("Querying Mirror Node: {}", url);

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode transfersNode = response.getBody().get("transactions");
                if (transfersNode != null && transfersNode.isArray()) {
                    return objectMapper.convertValue(transfersNode, objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
                }
            }
        } catch (Exception e) {
            log.error("Error querying Hedera Mirror Node for account {}: {}", accountId, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MirrorNodeQueryError", "Failed to query Hedera Mirror Node: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
