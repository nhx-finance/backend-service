package com.javaguy.nhxserver.model.dto;

import com.javaguy.nhxserver.model.entity.Asset;
import java.time.LocalDateTime;

public record AssetDto(
        Long id,
        String assetType,
        String name,
        String assetTicker,
        LocalDateTime dateUpdated
) {
    public static AssetDto fromEntity(Asset asset) {
        return new AssetDto(
                asset.getId(),
                asset.getAssetType(),
                asset.getName(),
                asset.getAssetTicker(),
                asset.getDateUpdated()
        );
    }
}
