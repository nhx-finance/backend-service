package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.ProductAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAccessRepository extends JpaRepository<ProductAccess, Long> {
    List<ProductAccess> findByUserUserId(Long userId);
}
