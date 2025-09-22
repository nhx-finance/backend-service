package com.javaguy.nhxserver.config;

import com.javaguy.nhxserver.model.entity.ERole;
import com.javaguy.nhxserver.model.entity.Role;
import com.javaguy.nhxserver.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import org.springframework.transaction.annotation.Transactional;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

        private final RoleRepository roleRepository;

        @Override
        @Transactional
        public void run(String... args) {
            log.info("Initializing database with default roles...");
            initializeRoles();
            log.info("Database initialization completed successfully");
        }

        private void initializeRoles() {
            for (ERole roleEnum : ERole.values()) {
                if (!roleRepository.existsByName(roleEnum)) {
                    Role role = new Role();
                    role.setName(roleEnum);
                    roleRepository.save(role);
                    log.info("Created role: {}", roleEnum.name());
                } else {
                    log.debug("Role already exists: {}", roleEnum.name());
                }
            }

            // Verify roles were created
            long roleCount = roleRepository.count();
            log.info("Total roles in database: {}", roleCount);
        }
}
