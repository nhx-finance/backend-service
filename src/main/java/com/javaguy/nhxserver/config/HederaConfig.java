package com.javaguy.nhxserver.config;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.javaguy.nhxserver.utils.HederaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HederaConfig {

    @Value("${hedera.network:testnet}")
    private String hederaNetwork;

    @Value("${hedera.operator-id}")
    private String operatorId;

    @Value("${hedera.operator-key}")
    private String operatorKey;

    @Value("${hedera.mirror-node-url}")
    private String mirrorNodeUrl;

    @Value("${hedera.transaction-timeout-seconds}")
    private long transactionTimeoutSeconds;

    @Value("${hedera.token.usdc}")
    private String usdcTokenId;

    @Value("${hedera.token.nhsaf}")
    private String nhsafTokenId;

    @Value("${hedera.treasury-account-id}")
    private String treasuryAccountId;

    @Bean
    public Client hederaClient() {
        Client client;
        if ("mainnet".equalsIgnoreCase(hederaNetwork)) {
            client = Client.forMainnet();
        } else {
            client = Client.forTestnet();
        }

        client.setOperator(AccountId.fromString(operatorId), PrivateKey.fromString(operatorKey));
        client.setRequestTimeout(Duration.ofSeconds(transactionTimeoutSeconds));

        return client;
    }

    @Bean
    public RestTemplate mirrorNodeRestTemplate() {
        return new RestTemplate();
    }

    public String getUsdcTokenId() {
        return usdcTokenId;
    }

    public String getNhsafTokenId() {
        return nhsafTokenId;
    }

    public String getTreasuryAccountId() {
        return treasuryAccountId;
    }

    public String getOperatorKey() {
        return operatorKey;
    }

    public String getOperatorId() {
        return operatorId;
    }
}
