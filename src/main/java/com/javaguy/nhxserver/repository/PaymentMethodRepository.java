package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserUserId(Long userId);
    Optional<PaymentMethod> findByUserUserIdAndName(Long userId, String name);
}
