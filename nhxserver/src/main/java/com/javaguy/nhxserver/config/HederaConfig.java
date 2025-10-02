package com.javaguy.nhxserver.config;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HederaConfig {

    @Value("${hedera.testnet.accountId}")
    private String hederaAccountId;

    @Value("${hedera.testnet.privateKey}")
    private String hederaPrivateKey;

    @Bean
    public Client hederaClient() {
        Client client = Client.forTestnet();
        client.setOperator(AccountId.fromString(hederaAccountId), PrivateKey.fromString(hederaPrivateKey));
        return client;
    }
}
