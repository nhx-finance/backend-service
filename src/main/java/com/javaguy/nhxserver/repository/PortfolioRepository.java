package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// import java.util.UUID; // Removed unused import

/**
 * Portfolio Repository
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * Find portfolio by user ID and token ID
     */
    Optional<Portfolio> findByUserIdAndTokenId(Long userId, String tokenId); // Changed to Long

    /**
     * Find all portfolios for a user
     */
    List<Portfolio> findByUserId(Long userId); // Changed to Long

    /**
     * Find all portfolios for a specific token
     */
    List<Portfolio> findByTokenId(String tokenId);

    /**
     * Get distinct user IDs that have portfolios
     */
    @Query("SELECT DISTINCT p.userId FROM Portfolio p")
    List<Long> findDistinctUserIds(); // Changed to Long

    /**
     * Find portfolios with balance greater than zero
     */
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.balance > 0")
    List<Portfolio> findActivePortfoliosByUserId(@Param("userId") Long userId); // Changed to Long

    /**
     * Get total balance for a token across all users
     */
    @Query("SELECT COALESCE(SUM(p.balance), 0) FROM Portfolio p WHERE p.tokenId = :tokenId")
    java.math.BigDecimal getTotalBalanceForToken(@Param("tokenId") String tokenId);
}
