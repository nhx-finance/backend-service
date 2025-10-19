package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findByUserUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
