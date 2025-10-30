package com.javaguy.nhxserver.config;

import com.javaguy.nhxserver.model.enums.ERole;
import com.javaguy.nhxserver.model.entity.Role;
import com.javaguy.nhxserver.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Profile("prod")
public class DatabaseInitializer implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

        @Autowired
        private RoleRepository roleRepository;

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
                    Role role = new Role(roleEnum);
                    roleRepository.save(role);
                    log.info("Created role: {}", roleEnum.name());
                } else {
                    log.debug("Role already exists: {}", roleEnum.name());
                }
            }
            long roleCount = roleRepository.count();
            log.info("Total roles in database: {}", roleCount);
        }
}
