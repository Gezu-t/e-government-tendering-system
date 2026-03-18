package com.egov.tendering.evaluation.integration;

import com.egov.tendering.dto.BidDTO;
import com.egov.tendering.dto.TenderCriteriaDTO;
import com.egov.tendering.dto.TenderDTO;
import com.egov.tendering.evaluation.client.BidClient;
import com.egov.tendering.evaluation.client.TenderClient;
import com.egov.tendering.evaluation.dal.dto.ConflictDeclarationRequest;
import com.egov.tendering.evaluation.dal.dto.ConflictOfInterestDTO;
import com.egov.tendering.evaluation.dal.dto.CriteriaScoreRequest;
import com.egov.tendering.evaluation.dal.dto.EvaluationCategoryConfigDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationDTO;
import com.egov.tendering.evaluation.dal.dto.EvaluationRequest;
import com.egov.tendering.evaluation.dal.model.ScoreCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EvaluationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("evaluation_service_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private TenderClient tenderClient;

    @MockBean
    private BidClient bidClient;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        public JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("userId", 1L)
                    .claim("roles", "ROLE_ADMIN,ROLE_EVALUATOR,ROLE_TENDEREE,ROLE_COMMITTEE")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().permitAll()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .decoder(jwtDecoder())
                                    .jwtAuthenticationConverter(testJwtAuthenticationConverter())
                            )
                    );
            return http.build();
        }

        private JwtAuthenticationConverter testJwtAuthenticationConverter() {
            JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
            grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
            grantedAuthoritiesConverter.setAuthorityPrefix("");

            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
            return jwtAuthenticationConverter;
        }
    }

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

        // Disable OAuth2 resource server auto-config issuer URI validation
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:9999/auth");

        // Feign client names
        registry.add("app.feign.tender-service", () -> "tender-service");
        registry.add("app.feign.bid-service", () -> "bidding-service");
        registry.add("app.feign.user-service", () -> "user-service");

        // Kafka topics
        registry.add("app.kafka.topics.evaluation-created", () -> "evaluation-created-test");
        registry.add("app.kafka.topics.evaluation-updated", () -> "evaluation-updated-test");
        registry.add("app.kafka.topics.evaluation-status-changed", () -> "evaluation-status-changed-test");
        registry.add("app.kafka.topics.evaluation-deleted", () -> "evaluation-deleted-test");
        registry.add("app.kafka.topics.tender-evaluation-completed", () -> "tender-evaluation-completed-test");
        registry.add("app.kafka.topics.tender-evaluation-approved", () -> "tender-evaluation-approved-test");
        registry.add("app.kafka.topics.review-created", () -> "review-created-test");
        registry.add("app.kafka.topics.review-updated", () -> "review-updated-test");
        registry.add("app.kafka.topics.review-deleted", () -> "review-deleted-test");

        // Committee settings
        registry.add("app.committee.default-required-review-count", () -> "3");
        registry.add("app.committee.default-minimum-approval-count", () -> "3");
    }

    @BeforeEach
    void setupMocks() {
        TenderCriteriaDTO criteria1 = TenderCriteriaDTO.builder()
                .id(1L)
                .name("Technical Quality")
                .description("Technical quality of the bid")
                .weight(new BigDecimal("0.60"))
                .criteriaType("TECHNICAL")
                .evaluationMethod("SCORING")
                .build();

        TenderCriteriaDTO criteria2 = TenderCriteriaDTO.builder()
                .id(2L)
                .name("Financial Competitiveness")
                .description("Financial aspects of the bid")
                .weight(new BigDecimal("0.40"))
                .criteriaType("FINANCIAL")
                .evaluationMethod("SCORING")
                .build();

        TenderDTO tenderDTO = TenderDTO.builder()
                .id(100L)
                .title("Test Tender")
                .description("A test tender for integration testing")
                .status("EVALUATION")
                .criteria(Arrays.asList(criteria1, criteria2))
                .build();

        when(tenderClient.getTenderById(anyLong())).thenReturn(tenderDTO);

        BidDTO bidDTO = BidDTO.builder()
                .id(200L)
                .tenderId(100L)
                .tendererId(10L)
                .tendererName("Test Vendor Ltd")
                .status("SUBMITTED")
                .submissionTime(LocalDateTime.now().minusDays(1))
                .items(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();

        when(bidClient.getBidById(anyLong())).thenReturn(bidDTO);
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("test-token");
        return headers;
    }

    @Test
    void testCreateEvaluation() {
        CriteriaScoreRequest score1 = CriteriaScoreRequest.builder()
                .criteriaId(1L)
                .score(new BigDecimal("8.5"))
                .justification("Strong technical proposal with clear methodology")
                .build();

        CriteriaScoreRequest score2 = CriteriaScoreRequest.builder()
                .criteriaId(2L)
                .score(new BigDecimal("7.0"))
                .justification("Competitive pricing but slightly above average")
                .build();

        EvaluationRequest request = EvaluationRequest.builder()
                .bidId(200L)
                .criteriaScores(Arrays.asList(score1, score2))
                .comments("Overall good bid with strong technical aspects")
                .build();

        HttpEntity<EvaluationRequest> httpEntity = new HttpEntity<>(request, createAuthHeaders());

        ResponseEntity<EvaluationDTO> response = restTemplate.exchange(
                "/api/evaluations/tenders/100",
                HttpMethod.POST,
                httpEntity,
                EvaluationDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTenderId()).isEqualTo(100L);
        assertThat(response.getBody().getBidId()).isEqualTo(200L);
        assertThat(response.getBody().getOverallScore()).isNotNull();
        assertThat(response.getBody().getComments()).isEqualTo("Overall good bid with strong technical aspects");
    }

    @Test
    void testGetEvaluationById() {
        // First, create an evaluation
        CriteriaScoreRequest score1 = CriteriaScoreRequest.builder()
                .criteriaId(1L)
                .score(new BigDecimal("9.0"))
                .justification("Excellent technical approach")
                .build();

        EvaluationRequest createRequest = EvaluationRequest.builder()
                .bidId(301L)
                .criteriaScores(Collections.singletonList(score1))
                .comments("Excellent bid")
                .build();

        HttpEntity<EvaluationRequest> createEntity = new HttpEntity<>(createRequest, createAuthHeaders());

        ResponseEntity<EvaluationDTO> createResponse = restTemplate.exchange(
                "/api/evaluations/tenders/100",
                HttpMethod.POST,
                createEntity,
                EvaluationDTO.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Long evaluationId = createResponse.getBody().getId();

        // Then, retrieve it by ID
        HttpEntity<Void> getEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<EvaluationDTO> getResponse = restTemplate.exchange(
                "/api/evaluations/{evaluationId}",
                HttpMethod.GET,
                getEntity,
                EvaluationDTO.class,
                evaluationId);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(evaluationId);
        assertThat(getResponse.getBody().getTenderId()).isEqualTo(100L);
        assertThat(getResponse.getBody().getBidId()).isEqualTo(301L);
        assertThat(getResponse.getBody().getComments()).isEqualTo("Excellent bid");
    }

    @Test
    void testConfigureMultiCriteriaCategories() {
        EvaluationCategoryConfigDTO technicalConfig = EvaluationCategoryConfigDTO.builder()
                .category(ScoreCategory.TECHNICAL)
                .weight(new BigDecimal("40.0"))
                .passThreshold(new BigDecimal("5.0"))
                .mandatory(true)
                .description("Technical evaluation criteria")
                .build();

        EvaluationCategoryConfigDTO financialConfig = EvaluationCategoryConfigDTO.builder()
                .category(ScoreCategory.FINANCIAL)
                .weight(new BigDecimal("30.0"))
                .passThreshold(new BigDecimal("4.0"))
                .mandatory(true)
                .description("Financial evaluation criteria")
                .build();

        EvaluationCategoryConfigDTO experienceConfig = EvaluationCategoryConfigDTO.builder()
                .category(ScoreCategory.EXPERIENCE)
                .weight(new BigDecimal("30.0"))
                .passThreshold(new BigDecimal("3.0"))
                .mandatory(false)
                .description("Experience evaluation criteria")
                .build();

        List<EvaluationCategoryConfigDTO> configs = Arrays.asList(
                technicalConfig, financialConfig, experienceConfig);

        HttpEntity<List<EvaluationCategoryConfigDTO>> httpEntity =
                new HttpEntity<>(configs, createAuthHeaders());

        ResponseEntity<List<EvaluationCategoryConfigDTO>> response = restTemplate.exchange(
                "/api/multi-criteria/tenders/100/categories",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<List<EvaluationCategoryConfigDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);

        BigDecimal totalWeight = response.getBody().stream()
                .map(EvaluationCategoryConfigDTO::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalWeight).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    @Test
    void testConflictOfInterestDeclaration() {
        ConflictDeclarationRequest request = ConflictDeclarationRequest.builder()
                .hasConflict(true)
                .conflictDescription("I have a financial interest in one of the bidding organizations")
                .relatedOrganizationId(500L)
                .relationshipType("FINANCIAL_INTEREST")
                .declarationText("I hereby declare that I have a conflict of interest")
                .build();

        HttpEntity<ConflictDeclarationRequest> httpEntity =
                new HttpEntity<>(request, createAuthHeaders());

        ResponseEntity<ConflictOfInterestDTO> response = restTemplate.exchange(
                "/api/conflict-of-interest/tenders/100/declare",
                HttpMethod.POST,
                httpEntity,
                ConflictOfInterestDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTenderId()).isEqualTo(100L);
        assertThat(response.getBody().getHasConflict()).isTrue();
        assertThat(response.getBody().getConflictDescription())
                .isEqualTo("I have a financial interest in one of the bidding organizations");
        assertThat(response.getBody().getRelatedOrganizationId()).isEqualTo(500L);
        assertThat(response.getBody().getRelationshipType()).isEqualTo("FINANCIAL_INTEREST");
    }
}
