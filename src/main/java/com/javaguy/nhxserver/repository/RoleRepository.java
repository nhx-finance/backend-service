package com.javaguy.nhxserver.repository;

import com.javaguy.nhxserver.model.enums.ERole;
import com.javaguy.nhxserver.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);

    boolean existsByName(ERole roleEnum);
}
