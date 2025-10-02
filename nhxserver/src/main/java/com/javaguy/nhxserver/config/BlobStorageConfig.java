package com.javaguy.nhxserver.config;

import com.javaguy.nhxserver.service.AzureBlobStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
public class BlobStorageConfig {

    private final AzureBlobStorageService azureBlobStorageService;

    @PostConstruct
    public void init() {
        azureBlobStorageService.init();
    }
}
