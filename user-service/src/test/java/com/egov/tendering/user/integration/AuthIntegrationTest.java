package com.egov.tendering.user.integration;

import com.egov.tendering.user.dal.dto.AuthResponse;
import com.egov.tendering.user.dal.dto.LoginRequest;
import com.egov.tendering.user.dal.dto.OrganizationRequest;
import com.egov.tendering.user.dal.dto.RegistrationRequest;
import com.egov.tendering.user.dal.dto.UserDTO;
import com.egov.tendering.user.dal.model.OrganizationType;
import com.egov.tendering.user.dal.model.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("user_service_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Datasource
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Flyway
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Disable Eureka and Config Server
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");

        // Disable Kafka
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:0");
        registry.add("spring.kafka.consumer.auto-startup", () -> "false");

        // JWT properties
        registry.add("app.security.jwt.secret",
                () -> "test-secret-key-that-is-at-least-64-bytes-long-for-hs512-algorithm-requirement");
        registry.add("app.security.jwt.expiration", () -> "3600000");

        // Kafka topic
        registry.add("app.kafka.topics.user-events", () -> "user-events-test");
    }

    @Test
    void testLoginWithSeededUser() {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("admin")
                .password("admin123")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo("admin");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response.getBody().getUserId()).isNotNull();
    }

    @Test
    void testLoginWithInvalidCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("admin")
                .password("wrongpassword")
                .build();

        ResponseEntity<Object> response = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testRegisterNewUser() {
        RegistrationRequest registrationRequest = RegistrationRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .role(UserRole.EVALUATOR)
                .build();

        ResponseEntity<UserDTO> response = restTemplate.postForEntity(
                "/api/auth/register", registrationRequest, UserDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("newuser");
        assertThat(response.getBody().getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.EVALUATOR);
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void testRegisterTendererWithOrganization() {
        OrganizationRequest orgRequest = OrganizationRequest.builder()
                .name("Test Corp Ltd")
                .registrationNumber("REG-2024-TEST-001")
                .address("456 Test Street, Addis Ababa")
                .contactPerson("John Doe")
                .phone("+251911000099")
                .email("info@testcorp.com")
                .organizationType(OrganizationType.PRIVATE)
                .build();

        RegistrationRequest registrationRequest = RegistrationRequest.builder()
                .username("tenderer_new")
                .email("tenderer_new@example.com")
                .password("password123")
                .role(UserRole.TENDERER)
                .organization(orgRequest)
                .build();

        ResponseEntity<UserDTO> response = restTemplate.postForEntity(
                "/api/auth/register", registrationRequest, UserDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("tenderer_new");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.TENDERER);
        assertThat(response.getBody().getOrganizations()).isNotNull();
        assertThat(response.getBody().getOrganizations()).isNotEmpty();
    }

    @Test
    void testGetUserByIdWithToken() {
        // Step 1: Login to get a token
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("admin")
                .password("admin123")
                .build();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();

        String token = loginResponse.getBody().getToken();
        Long userId = loginResponse.getBody().getUserId();

        // Step 2: Use the token to access a protected endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<UserDTO> userResponse = restTemplate.exchange(
                "/api/users/{userId}", HttpMethod.GET, requestEntity, UserDTO.class, userId);

        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userResponse.getBody()).isNotNull();
        assertThat(userResponse.getBody().getId()).isEqualTo(userId);
        assertThat(userResponse.getBody().getUsername()).isEqualTo("admin");
        assertThat(userResponse.getBody().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void testAccessProtectedEndpointWithoutToken() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/api/users", Object.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
