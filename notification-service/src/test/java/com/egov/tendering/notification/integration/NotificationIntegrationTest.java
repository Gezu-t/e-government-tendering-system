package com.egov.tendering.notification.integration;

import com.egov.tendering.notification.dal.dto.NotificationRequest;
import com.egov.tendering.notification.dal.model.Notification;
import com.egov.tendering.notification.dal.model.NotificationType;
import com.egov.tendering.notification.dal.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        "spring.flyway.enabled=false",
        "app.security.jwt.secret=test-secret-key-that-is-at-least-64-bytes-long-for-hs512-algorithm-ok",
        "app.security.jwt.claim-roles=roles",
        // Disable actual mail sending
        "spring.mail.host=localhost",
        "spring.mail.port=3025"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    // Prevent actual SMTP connections
    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(testJwtAuthenticationConverter()))
                    );
            return http.build();
        }

        @Bean
        @Primary
        public JwtDecoder testJwtDecoder() {
            return token -> {
                Map<String, Object> headers = Map.of("alg", "none", "typ", "JWT");
                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", "1");
                claims.put("userId", 1L);
                claims.put("roles", List.of("ROLE_ADMIN"));
                return new Jwt(token, Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
            };
        }

        private JwtAuthenticationConverter testJwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                List<String> roles = jwt.getClaimAsStringList("roles");
                if (roles == null) return Collections.emptyList();
                return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            });
            return converter;
        }
    }

    private static Long createdNotificationId;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        return headers;
    }

    // ==================== Test 1: Send Notification ====================

    @Test
    @Order(1)
    void testSendNotification() {
        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.TENDER_PUBLISHED)
                .entityId("1")
                .subject("New Tender Published")
                .message("A new tender for office furniture supply has been published.")
                .recipients(List.of("vendor@example.com", "vendor2@example.com"))
                .build();

        HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/notifications", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        // Verify it was persisted
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).isNotEmpty();
        Notification saved = all.get(0);
        assertThat(saved.getSubject()).isEqualTo("New Tender Published");
        assertThat(saved.getRecipients()).containsExactlyInAnyOrder(
                "vendor@example.com", "vendor2@example.com");

        createdNotificationId = saved.getId();
    }

    // ==================== Test 2: Get Notification by ID ====================

    @Test
    @Order(2)
    void testGetNotificationById() {
        assertThat(createdNotificationId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/notifications/" + createdNotificationId,
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("New Tender Published");
    }

    // ==================== Test 3: Mark as Read ====================

    @Test
    @Order(3)
    void testMarkNotificationAsRead() {
        assertThat(createdNotificationId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/notifications/" + createdNotificationId + "/read",
                HttpMethod.PUT, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify it is now marked as read in the DB
        Notification notification = notificationRepository.findById(createdNotificationId).orElseThrow();
        assertThat(notification.isRead()).isTrue();
    }

    // ==================== Test 4: Unread count ====================

    @Test
    @Order(4)
    void testUnreadCount() {
        // Send a second notification that is still unread
        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.CONTRACT_AWARDED)
                .entityId("2")
                .subject("Contract Awarded")
                .message("Congratulations, your bid has been selected.")
                .recipients(List.of("winner@example.com"))
                .build();

        restTemplate.exchange("/api/notifications", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), String.class);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Long> response = restTemplate.exchange(
                "/api/notifications/count/unread/1",
                HttpMethod.GET, entity, Long.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // At least 1 unread (the contract awarded one just sent)
        assertThat(response.getBody()).isGreaterThanOrEqualTo(1L);
    }
}
