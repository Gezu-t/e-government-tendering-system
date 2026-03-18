package com.egov.tendering.contract.integration;

import com.egov.tendering.contract.dal.dto.*;
import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentStatus;
import com.egov.tendering.contract.dal.model.ContractAmendment.AmendmentType;
import com.egov.tendering.contract.dal.model.ContractStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
        "app.security.jwt.claim-roles=roles"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContractIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("contract_test")
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
                claims.put("sub", "test-user");
                claims.put("userId", 1L);
                claims.put("roles", List.of("ROLE_ADMIN", "ROLE_TENDEREE", "ROLE_TENDERER"));
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

    private static Long createdContractId;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        return headers;
    }

    private CreateContractRequest buildCreateContractRequest() {
        CreateContractRequest.ContractItemRequest item = CreateContractRequest.ContractItemRequest.builder()
                .tenderItemId(1L)
                .name("Office Desks")
                .description("Standard office desks")
                .quantity(10)
                .unit("units")
                .unitPrice(new BigDecimal("500.00"))
                .build();

        CreateContractRequest.ContractMilestoneRequest milestone = CreateContractRequest.ContractMilestoneRequest.builder()
                .title("Delivery Phase 1")
                .description("Initial delivery of 5 desks")
                .dueDate(LocalDate.now().plusDays(30))
                .paymentAmount(new BigDecimal("2500.00"))
                .build();

        return CreateContractRequest.builder()
                .tenderId(1L)
                .bidderId(2L)
                .contractNumber("CNT-2026-001")
                .title("Office Furniture Supply Contract")
                .description("Contract for supply of office furniture")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .items(List.of(item))
                .milestones(List.of(milestone))
                .build();
    }

    // ==================== Test 1: Create Contract ====================

    @Test
    @Order(1)
    void testCreateContract() {
        HttpEntity<CreateContractRequest> entity = new HttpEntity<>(buildCreateContractRequest(), authHeaders());

        ResponseEntity<ContractDTO> response = restTemplate.exchange(
                "/api/contracts", HttpMethod.POST, entity, ContractDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ContractDTO contract = response.getBody();
        assertThat(contract).isNotNull();
        assertThat(contract.getId()).isNotNull();
        assertThat(contract.getContractNumber()).isEqualTo("CNT-2026-001");
        assertThat(contract.getTitle()).isEqualTo("Office Furniture Supply Contract");
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(contract.getTenderId()).isEqualTo(1L);
        assertThat(contract.getBidderId()).isEqualTo(2L);
        assertThat(contract.getItems()).hasSize(1);
        assertThat(contract.getMilestones()).hasSize(1);

        createdContractId = contract.getId();
    }

    // ==================== Test 2: Get Contract by ID ====================

    @Test
    @Order(2)
    void testGetContractById() {
        assertThat(createdContractId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<ContractDTO> response = restTemplate.exchange(
                "/api/contracts/" + createdContractId, HttpMethod.GET, entity, ContractDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ContractDTO contract = response.getBody();
        assertThat(contract).isNotNull();
        assertThat(contract.getId()).isEqualTo(createdContractId);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
    }

    // ==================== Test 3: Get Contracts by Tender ID ====================

    @Test
    @Order(3)
    void testGetContractsByTenderId() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/contracts/tender/1", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("CNT-2026-001");
    }

    // ==================== Test 4: Activate Contract ====================

    @Test
    @Order(4)
    void testActivateContract() {
        assertThat(createdContractId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<ContractDTO> response = restTemplate.exchange(
                "/api/contracts/" + createdContractId + "/activate",
                HttpMethod.POST, entity, ContractDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }

    // ==================== Test 5: Add Milestone ====================

    @Test
    @Order(5)
    void testAddMilestone() {
        assertThat(createdContractId).isNotNull();

        ContractMilestoneDTO milestone = ContractMilestoneDTO.builder()
                .title("Delivery Phase 2")
                .description("Final delivery of remaining 5 desks")
                .dueDate(LocalDate.now().plusDays(60))
                .paymentAmount(new BigDecimal("2500.00"))
                .build();

        HttpEntity<ContractMilestoneDTO> entity = new HttpEntity<>(milestone, authHeaders());

        ResponseEntity<ContractMilestoneDTO> response = restTemplate.exchange(
                "/api/contracts/" + createdContractId + "/milestones",
                HttpMethod.POST, entity, ContractMilestoneDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Delivery Phase 2");
    }

    // ==================== Test 6: Request Amendment ====================

    @Test
    @Order(6)
    void testRequestAmendment() {
        assertThat(createdContractId).isNotNull();

        ContractAmendmentRequest amendmentRequest = ContractAmendmentRequest.builder()
                .type(AmendmentType.TIMELINE_EXTENSION)
                .reason("Supplier requires additional lead time")
                .description("Extending contract end date by 30 days")
                .newEndDate(LocalDate.now().plusYears(1).plusDays(30))
                .build();

        HttpEntity<ContractAmendmentRequest> entity = new HttpEntity<>(amendmentRequest, authHeaders());

        ResponseEntity<ContractAmendmentDTO> response = restTemplate.exchange(
                "/api/contracts/" + createdContractId + "/amendments",
                HttpMethod.POST, entity, ContractAmendmentDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReason()).isEqualTo("Supplier requires additional lead time");
        assertThat(response.getBody().getStatus()).isEqualTo(AmendmentStatus.PENDING);
    }

    // ==================== Test 7: Full Lifecycle ====================

    @Test
    @Order(7)
    void testFullContractLifecycle() {
        // Create a fresh contract
        CreateContractRequest request = buildCreateContractRequest();
        request.setContractNumber("CNT-2026-002");
        request.setTitle("Lifecycle Test Contract");
        HttpEntity<CreateContractRequest> createEntity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<ContractDTO> createResponse = restTemplate.exchange(
                "/api/contracts", HttpMethod.POST, createEntity, ContractDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long contractId = createResponse.getBody().getId();

        // Activate
        ResponseEntity<ContractDTO> activateResponse = restTemplate.exchange(
                "/api/contracts/" + contractId + "/activate",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), ContractDTO.class);
        assertThat(activateResponse.getBody().getStatus()).isEqualTo(ContractStatus.ACTIVE);

        // Complete
        ResponseEntity<ContractDTO> completeResponse = restTemplate.exchange(
                "/api/contracts/" + contractId + "/complete",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), ContractDTO.class);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResponse.getBody().getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }
}
