package com.egov.tendering.user.config;

import com.egov.tendering.user.dal.model.User;
import com.egov.tendering.user.dal.model.UserRole;
import com.egov.tendering.user.dal.model.UserStatus;
import com.egov.tendering.user.dal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds default users on first startup if they don't already exist.
 *
 * Default credentials (change in production via environment variables):
 *
 *   ADMIN:     admin / admin123
 *   TENDEREE:  tenderee / tenderee123
 *   TENDERER:  tenderer / tenderer123
 *   EVALUATOR: evaluator / evaluator123
 *   COMMITTEE: committee / committee123
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.seed.admin.email:admin@egov.gov}")
    private String adminEmail;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Bean
    CommandLineRunner initDefaultUsers() {
        return args -> {
            if (!seedEnabled) {
                log.info("Data seeding is disabled (app.seed.enabled=false)");
                return;
            }

            createUserIfNotExists(adminUsername, adminEmail, adminPassword, UserRole.ADMIN);
            createUserIfNotExists("tenderee", "tenderee@egov.gov", "tenderee123", UserRole.TENDEREE);
            createUserIfNotExists("tenderer", "tenderer@egov.gov", "tenderer123", UserRole.TENDERER);
            createUserIfNotExists("evaluator", "evaluator@egov.gov", "evaluator123", UserRole.EVALUATOR);
            createUserIfNotExists("committee", "committee@egov.gov", "committee123", UserRole.COMMITTEE);

            log.info("Default user seeding complete");
        };
    }

    private void createUserIfNotExists(String username, String email, String password, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("User '{}' already exists, skipping", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        log.info("Created default {} user: {} / {}", role, username, password);
    }
}
