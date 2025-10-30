package com.javaguy.nhxserver.config;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@Getter
public class HederaConfig {

    @Value("${hedera.network:testnet}")
    private String hederaNetwork;
    @Value("${hedera.mirror-node-url}")
    private String mirrorNodeUrl;
    @Value("${hedera.transaction-timeout-seconds}")
    private long transactionTimeoutSeconds;
    @Value("${hedera.treasury-key}")
    private String treasuryKey;
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

        client.setOperator(AccountId.fromString(treasuryAccountId), PrivateKey.fromString(treasuryKey));
        client.setRequestTimeout(Duration.ofSeconds(transactionTimeoutSeconds));

        return client;
    }

    @Bean
    public RestTemplate mirrorNodeRestTemplate() {
        return new RestTemplate();
    }

}
