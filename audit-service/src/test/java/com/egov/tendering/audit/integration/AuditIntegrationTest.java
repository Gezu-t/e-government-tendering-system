package com.egov.tendering.audit.integration;

import com.egov.tendering.audit.dal.model.AuditActionType;
import com.egov.tendering.audit.dal.model.AuditLog;
import com.egov.tendering.audit.dal.repository.AuditLogRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.kafka.consumer.auto-startup=false",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("audit_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditLog buildAuditLog(AuditActionType actionType, String entityType, String module) {
        return AuditLog.builder()
                .userId(1L)
                .username("test-user")
                .actionType(actionType)
                .entityType(entityType)
                .entityId("100")
                .action(actionType.name())
                .details("Integration test audit entry")
                .sourceIp("127.0.0.1")
                .userAgent("test-agent")
                .success(true)
                .module(module)
                .correlationId("corr-001")
                .serviceId("audit-service")
                .build();
    }

    // ==================== Test 1: Empty procurement summary ====================

    @Test
    @Order(1)
    void testProcurementSummaryEmpty() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/reports/procurement-summary?from=2026-01-01&to=2026-01-31",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ==================== Test 2: Audit activity with seeded data ====================

    @Test
    @Order(2)
    void testAuditActivityWithData() {
        // Seed audit logs across different modules
        auditLogRepository.save(buildAuditLog(AuditActionType.TENDER_CREATED, "TENDER", "TENDER"));
        auditLogRepository.save(buildAuditLog(AuditActionType.BID_SUBMITTED, "BID", "BIDDING"));
        auditLogRepository.save(buildAuditLog(AuditActionType.CONTRACT_CREATED, "CONTRACT", "CONTRACT"));
        auditLogRepository.save(buildAuditLog(AuditActionType.USER_LOGIN, "USER", "USER"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/reports/audit-activity?from=2026-01-01&to=2026-12-31",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ==================== Test 3: Procurement summary with tender data ====================

    @Test
    @Order(3)
    void testProcurementSummaryWithTenderData() {
        // Seed tender-related audit logs
        for (int i = 0; i < 3; i++) {
            auditLogRepository.save(buildAuditLog(AuditActionType.TENDER_PUBLISHED, "TENDER", "TENDER"));
        }
        for (int i = 0; i < 2; i++) {
            auditLogRepository.save(buildAuditLog(AuditActionType.CONTRACT_SIGNED, "CONTRACT", "CONTRACT"));
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/reports/procurement-summary?from=2026-01-01&to=2026-12-31",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ==================== Test 4: Repository count verification ====================

    @Test
    @Order(4)
    void testAuditLogsPersisted() {
        long count = auditLogRepository.count();
        // Tests 2 and 3 inserted 4 + 5 = 9 entries
        assertThat(count).isGreaterThanOrEqualTo(9);
    }
}
