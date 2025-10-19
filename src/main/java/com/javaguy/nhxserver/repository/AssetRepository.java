package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByUserUserId(Long userId);
}
