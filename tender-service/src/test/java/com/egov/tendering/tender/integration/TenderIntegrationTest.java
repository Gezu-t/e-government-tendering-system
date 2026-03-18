package com.egov.tendering.tender.integration;

import com.egov.tendering.tender.dal.dto.*;
import com.egov.tendering.tender.dal.model.AllocationStrategy;
import com.egov.tendering.tender.dal.model.CriteriaType;
import com.egov.tendering.tender.dal.model.PreBidClarification.ClarificationStatus;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.model.TenderType;
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
import java.time.LocalDateTime;
import java.util.*;

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
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.flyway.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("tender_test")
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

    /**
     * Test configuration that disables OAuth2 JWT security and permits all requests.
     * A fake JwtDecoder is provided so that the Authorization header with a Bearer token
     * is decoded into a Jwt principal that carries the userId claim and ROLE_ADMIN authority.
     */
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
            // Return a fake JwtDecoder that creates a Jwt with required claims
            return token -> {
                Map<String, Object> headers = Map.of("alg", "none", "typ", "JWT");
                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", "test-user");
                claims.put("userId", 1L);
                claims.put("roles", List.of("ROLE_ADMIN", "ROLE_TENDEREE", "ROLE_TENDERER"));
                return new Jwt(token, Instant.now(), Instant.now().plusSeconds(3600),
                        headers, claims);
            };
        }

        private JwtAuthenticationConverter testJwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                List<String> roles = jwt.getClaimAsStringList("roles");
                if (roles == null) {
                    return Collections.emptyList();
                }
                return roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(java.util.stream.Collectors.toList());
            });
            return converter;
        }
    }

    // Shared state for ordered lifecycle tests
    private static Long createdTenderId;
    // Tracks the next expected auto-increment ID for criteria, so items can reference them
    private static long nextCriteriaId = 1;

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        return headers;
    }

    private CreateTenderRequest buildCreateTenderRequest() {
        TenderCriteriaRequest criteria = TenderCriteriaRequest.builder()
                .name("Price")
                .description("Unit price evaluation")
                .type(CriteriaType.PRICE)
                .weight(new BigDecimal("60.00"))
                .preferHigher(false)
                .build();

        // The service saves criteria first (auto-generating IDs), then matches items
        // to criteria by criteriaId. We track the next expected auto-increment ID via a
        // static counter so that each call to this helper produces a valid criteriaId reference.
        long currentCriteriaId = nextCriteriaId++;
        TenderItemRequest item = TenderItemRequest.builder()
                .criteriaId(currentCriteriaId)
                .name("Office Desks")
                .description("Standard office desks")
                .quantity(50)
                .unit("units")
                .estimatedPrice(new BigDecimal("5000.00"))
                .build();

        return CreateTenderRequest.builder()
                .title("Integration Test Tender")
                .description("A tender created by integration tests")
                .type(TenderType.OPEN)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .allocationStrategy(AllocationStrategy.SINGLE)
                .minWinners(1)
                .maxWinners(1)
                .cutoffScore(new BigDecimal("6.00"))
                .isAverageAllocation(false)
                .criteria(List.of(criteria))
                .items(List.of(item))
                .build();
    }

    // ==================== Test 1: Create Tender ====================

    @Test
    @Order(1)
    void testCreateTender() {
        CreateTenderRequest request = buildCreateTenderRequest();
        HttpEntity<CreateTenderRequest> entity = new HttpEntity<>(request, createAuthHeaders());

        ResponseEntity<TenderDTO> response = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                entity,
                TenderDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        TenderDTO tender = response.getBody();
        assertThat(tender.getId()).isNotNull();
        assertThat(tender.getTitle()).isEqualTo("Integration Test Tender");
        assertThat(tender.getDescription()).isEqualTo("A tender created by integration tests");
        assertThat(tender.getStatus()).isEqualTo(TenderStatus.DRAFT);
        assertThat(tender.getType()).isEqualTo(TenderType.OPEN);
        assertThat(tender.getAllocationStrategy()).isEqualTo(AllocationStrategy.SINGLE);
        assertThat(tender.getTendereeId()).isEqualTo(1L);
        assertThat(tender.getCreatedAt()).isNotNull();

        createdTenderId = tender.getId();
    }

    // ==================== Test 2: Get Tender by ID ====================

    @Test
    @Order(2)
    void testGetTenderById() {
        assertThat(createdTenderId).isNotNull();

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<TenderDTO> response = restTemplate.exchange(
                "/api/tenders/" + createdTenderId,
                HttpMethod.GET,
                entity,
                TenderDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        TenderDTO tender = response.getBody();
        assertThat(tender.getId()).isEqualTo(createdTenderId);
        assertThat(tender.getTitle()).isEqualTo("Integration Test Tender");
        assertThat(tender.getStatus()).isEqualTo(TenderStatus.DRAFT);
    }

    // ==================== Test 3: Search Tenders ====================

    @Test
    @Order(3)
    void testSearchTenders() {
        // First, publish the tender so it appears in public search results
        HttpEntity<Void> publishEntity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(
                "/api/tenders/" + createdTenderId + "/publish",
                HttpMethod.POST,
                publishEntity,
                TenderDTO.class
        );

        // Now search for published tenders
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tenders?status=PUBLISHED&size=10",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // The response is a Page<TenderDTO>, verify it contains content
        assertThat(response.getBody()).contains("Integration Test Tender");

    }

    // ==================== Test 4: Publish Tender ====================

    @Test
    @Order(4)
    void testPublishTender() {
        // Create a fresh tender for this test
        CreateTenderRequest request = buildCreateTenderRequest();
        request.setTitle("Tender for Publish Test");
        HttpEntity<CreateTenderRequest> createEntity = new HttpEntity<>(request, createAuthHeaders());

        ResponseEntity<TenderDTO> createResponse = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity,
                TenderDTO.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long tenderId = createResponse.getBody().getId();
        assertThat(createResponse.getBody().getStatus()).isEqualTo(TenderStatus.DRAFT);

        // Publish the tender
        HttpEntity<Void> publishEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<TenderDTO> response = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/publish",
                HttpMethod.POST,
                publishEntity,
                TenderDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TenderStatus.PUBLISHED);
        assertThat(response.getBody().getId()).isEqualTo(tenderId);
    }

    // ==================== Test 5: Amend Tender ====================

    @Test
    @Order(5)
    void testAmendTender() {
        // Create and publish a tender first
        CreateTenderRequest createRequest = buildCreateTenderRequest();
        createRequest.setTitle("Tender for Amend Test");
        HttpEntity<CreateTenderRequest> createEntity = new HttpEntity<>(createRequest, createAuthHeaders());

        ResponseEntity<TenderDTO> createResponse = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity,
                TenderDTO.class
        );
        Long tenderId = createResponse.getBody().getId();

        // Publish it
        HttpEntity<Void> publishEntity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(
                "/api/tenders/" + tenderId + "/publish",
                HttpMethod.POST,
                publishEntity,
                TenderDTO.class
        );

        // Amend the tender
        TenderAmendmentRequest amendRequest = TenderAmendmentRequest.builder()
                .reason("Extending deadline due to additional requirements")
                .description("Updated description after amendment")
                .newSubmissionDeadline(LocalDateTime.now().plusDays(60))
                .build();

        HttpEntity<TenderAmendmentRequest> amendEntity = new HttpEntity<>(amendRequest, createAuthHeaders());

        ResponseEntity<TenderDTO> response = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/amend",
                HttpMethod.POST,
                amendEntity,
                TenderDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TenderStatus.AMENDED);
        assertThat(response.getBody().getDescription()).isEqualTo("Updated description after amendment");
    }

    // ==================== Test 6: Close Tender ====================

    @Test
    @Order(6)
    void testCloseTender() {
        // Create and publish a tender
        CreateTenderRequest createRequest = buildCreateTenderRequest();
        createRequest.setTitle("Tender for Close Test");
        HttpEntity<CreateTenderRequest> createEntity = new HttpEntity<>(createRequest, createAuthHeaders());

        ResponseEntity<TenderDTO> createResponse = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity,
                TenderDTO.class
        );
        Long tenderId = createResponse.getBody().getId();

        // Publish it
        HttpEntity<Void> publishEntity = new HttpEntity<>(createAuthHeaders());
        restTemplate.exchange(
                "/api/tenders/" + tenderId + "/publish",
                HttpMethod.POST,
                publishEntity,
                TenderDTO.class
        );

        // Close the tender
        HttpEntity<Void> closeEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<TenderDTO> response = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/close",
                HttpMethod.POST,
                closeEntity,
                TenderDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TenderStatus.CLOSED);
    }

    // ==================== Test 7: Full Lifecycle ====================

    @Test
    @Order(7)
    void testFullLifecycle() {
        HttpHeaders headers = createAuthHeaders();

        // Step 1: Create a tender
        CreateTenderRequest createRequest = buildCreateTenderRequest();
        createRequest.setTitle("Full Lifecycle Tender");
        HttpEntity<CreateTenderRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<TenderDTO> createResponse = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity,
                TenderDTO.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TenderDTO tender = createResponse.getBody();
        assertThat(tender).isNotNull();
        assertThat(tender.getStatus()).isEqualTo(TenderStatus.DRAFT);
        Long tenderId = tender.getId();

        // Step 2: Publish the tender
        HttpEntity<Void> publishEntity = new HttpEntity<>(headers);
        ResponseEntity<TenderDTO> publishResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/publish",
                HttpMethod.POST,
                publishEntity,
                TenderDTO.class
        );

        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody().getStatus()).isEqualTo(TenderStatus.PUBLISHED);

        // Step 3: Amend the tender
        TenderAmendmentRequest amendRequest = TenderAmendmentRequest.builder()
                .reason("Scope change requiring additional items")
                .description("Amended description for lifecycle test")
                .newSubmissionDeadline(LocalDateTime.now().plusDays(45))
                .build();

        HttpEntity<TenderAmendmentRequest> amendEntity = new HttpEntity<>(amendRequest, headers);
        ResponseEntity<TenderDTO> amendResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/amend",
                HttpMethod.POST,
                amendEntity,
                TenderDTO.class
        );

        assertThat(amendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(amendResponse.getBody().getStatus()).isEqualTo(TenderStatus.AMENDED);

        // Verify amendment was recorded
        HttpEntity<Void> getAmendmentsEntity = new HttpEntity<>(headers);
        ResponseEntity<String> amendmentsResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/amendments",
                HttpMethod.GET,
                getAmendmentsEntity,
                String.class
        );
        assertThat(amendmentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(amendmentsResponse.getBody()).contains("Scope change requiring additional items");

        // Step 4: AMENDED tenders can only transition to CANCELLED per the allowed
        // status transitions (closeTender requires PUBLISHED). Cancel the amended tender.
        UpdateTenderStatusRequest cancelRequest = new UpdateTenderStatusRequest();
        cancelRequest.setStatus(TenderStatus.CANCELLED);

        HttpEntity<UpdateTenderStatusRequest> cancelEntity = new HttpEntity<>(cancelRequest, headers);
        ResponseEntity<TenderDTO> cancelResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/status",
                HttpMethod.PATCH,
                cancelEntity,
                TenderDTO.class
        );

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(TenderStatus.CANCELLED);

        // Also test the Create -> Publish -> Close path
        CreateTenderRequest createRequest2 = buildCreateTenderRequest();
        createRequest2.setTitle("Lifecycle Close Path Tender");
        HttpEntity<CreateTenderRequest> createEntity2 = new HttpEntity<>(createRequest2, headers);

        ResponseEntity<TenderDTO> createResponse2 = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity2,
                TenderDTO.class
        );
        Long tenderId2 = createResponse2.getBody().getId();

        // Publish
        ResponseEntity<TenderDTO> publishResponse2 = restTemplate.exchange(
                "/api/tenders/" + tenderId2 + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                TenderDTO.class
        );
        assertThat(publishResponse2.getBody().getStatus()).isEqualTo(TenderStatus.PUBLISHED);

        // Close
        ResponseEntity<TenderDTO> closeResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId2 + "/close",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                TenderDTO.class
        );
        assertThat(closeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(closeResponse.getBody().getStatus()).isEqualTo(TenderStatus.CLOSED);
    }

    // ==================== Test 8: Pre-Bid Clarification ====================

    @Test
    @Order(8)
    void testPreBidClarification() {
        HttpHeaders headers = createAuthHeaders();

        // Create and publish a tender for clarifications
        CreateTenderRequest createRequest = buildCreateTenderRequest();
        createRequest.setTitle("Tender for Clarification Test");
        HttpEntity<CreateTenderRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<TenderDTO> createResponse = restTemplate.exchange(
                "/api/tenders",
                HttpMethod.POST,
                createEntity,
                TenderDTO.class
        );
        Long tenderId = createResponse.getBody().getId();

        // Publish so questions can be asked
        restTemplate.exchange(
                "/api/tenders/" + tenderId + "/publish",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                TenderDTO.class
        );

        // Step 1: POST a question
        PreBidQuestionRequest questionRequest = PreBidQuestionRequest.builder()
                .question("What are the minimum hardware specifications required?")
                .category("Technical")
                .organizationName("Test Corp")
                .build();

        HttpEntity<PreBidQuestionRequest> questionEntity = new HttpEntity<>(questionRequest, headers);
        ResponseEntity<PreBidClarificationDTO> questionResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/clarifications",
                HttpMethod.POST,
                questionEntity,
                PreBidClarificationDTO.class
        );

        assertThat(questionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(questionResponse.getBody()).isNotNull();

        PreBidClarificationDTO clarification = questionResponse.getBody();
        assertThat(clarification.getQuestion()).isEqualTo("What are the minimum hardware specifications required?");
        assertThat(clarification.getStatus()).isEqualTo(ClarificationStatus.PENDING);
        assertThat(clarification.getAskedByOrgName()).isEqualTo("Test Corp");
        assertThat(clarification.getCategory()).isEqualTo("Technical");
        Long clarId = clarification.getId();

        // Step 2: GET public clarifications (should be empty since none are answered yet)
        ResponseEntity<String> publicResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/clarifications/public",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(publicResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // No answered clarifications yet, so the list should be empty
        assertThat(publicResponse.getBody()).isEqualTo("[]");

        // Step 3: PUT answer to the question
        PreBidAnswerRequest answerRequest = PreBidAnswerRequest.builder()
                .answer("Minimum specs: Intel i5, 8GB RAM, 256GB SSD")
                .makePublic(true)
                .build();

        HttpEntity<PreBidAnswerRequest> answerEntity = new HttpEntity<>(answerRequest, headers);
        ResponseEntity<PreBidClarificationDTO> answerResponse = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/clarifications/" + clarId + "/answer",
                HttpMethod.PUT,
                answerEntity,
                PreBidClarificationDTO.class
        );

        assertThat(answerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(answerResponse.getBody()).isNotNull();
        assertThat(answerResponse.getBody().getAnswer()).isEqualTo("Minimum specs: Intel i5, 8GB RAM, 256GB SSD");
        assertThat(answerResponse.getBody().getStatus()).isEqualTo(ClarificationStatus.ANSWERED);
        assertThat(answerResponse.getBody().getAnsweredBy()).isNotNull();

        // Step 4: GET public clarifications again (should now contain the answered one)
        ResponseEntity<String> publicResponse2 = restTemplate.exchange(
                "/api/tenders/" + tenderId + "/clarifications/public",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(publicResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicResponse2.getBody()).contains("Minimum specs: Intel i5, 8GB RAM, 256GB SSD");
        assertThat(publicResponse2.getBody()).contains("What are the minimum hardware specifications required?");
    }
}
