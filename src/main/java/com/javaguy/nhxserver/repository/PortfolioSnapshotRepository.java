package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Portfolio Snapshot Repository
 */
@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    /**
     * Find snapshots by user ID, ordered by time descending
     */
    List<PortfolioSnapshot> findByUserIdOrderBySnapshotTimeDesc(Long userId);

    /**
     * Find snapshots by user ID, ordered by time ascending
     */
    List<PortfolioSnapshot> findByUserIdOrderBySnapshotTimeAsc(Long userId);

    /**
     * Find snapshots by user ID within date range
     */
    List<PortfolioSnapshot> findByUserIdAndSnapshotTimeBetweenOrderBySnapshotTimeDesc(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find latest snapshot for user
     */
    Optional<PortfolioSnapshot> findTopByUserIdOrderBySnapshotTimeDesc(Long userId);

    /**
     * Find snapshots after specific time
     */
    List<PortfolioSnapshot> findBySnapshotTimeAfterOrderBySnapshotTimeDesc(LocalDateTime time);

    /**
     * Delete old snapshots before specific date
     */
    void deleteBySnapshotTimeBefore(LocalDateTime cutoffTime);

    /**
     * Count snapshots for user
     */
    long countByUserId(Long userId);
}
