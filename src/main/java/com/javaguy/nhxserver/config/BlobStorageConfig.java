package com.javaguy.nhxserver.config;

import com.javaguy.nhxserver.service.azure.AzureBlobStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BlobStorageConfig {

    private final AzureBlobStorageService azureBlobStorageService;

    @PostConstruct
    public void init() {
        azureBlobStorageService.init();
    }
}
