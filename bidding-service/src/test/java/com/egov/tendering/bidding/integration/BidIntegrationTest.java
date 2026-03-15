package com.egov.tendering.bidding.integration;

import com.egov.tendering.bidding.client.TenderClient;
import com.egov.tendering.bidding.config.BidAccessSecurityUtil;
import com.egov.tendering.bidding.config.JwtUserIdExtractor;
import com.egov.tendering.bidding.dal.dto.BidDTO;
import com.egov.tendering.bidding.dal.dto.BidItemRequest;
import com.egov.tendering.bidding.dal.dto.BidSubmissionRequest;
import com.egov.tendering.bidding.dal.dto.PageDTO;
import com.egov.tendering.bidding.dal.model.BidStatus;
import com.egov.tendering.bidding.event.BidEventPublisher;
import com.egov.tendering.bidding.service.TenderWorkflowGuard;
import com.egov.tendering.dto.TenderDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.kafka.bootstrap-servers=localhost:0",
                "spring.kafka.consumer.auto-startup=false",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "app.kafka.topics.bid-events=bid-events-test",
                "app.kafka.topics.tender-events=tender-events-test",
                "app.kafka.topics.evaluation-events=evaluation-events-test",
                "app.kafka.topics.tender-evaluation-completed=tender-evaluation-completed-test",
                "app.kafka.topics.contract-events=contract-events-test",
                "app.file-storage.upload-dir=/tmp/test-uploads",
                "app.feign.tender-service=tender-service"
        })
@Testcontainers
class BidIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("bidding_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2ResourceServer(AbstractHttpConfigurer::disable);
            return http.build();
        }

        @Bean
        @Primary
        public JwtDecoder testJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("userId", 1L)
                    .claim("roles", List.of("ROLE_TENDERER", "ROLE_ADMIN", "ROLE_EVALUATOR"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Bean
        @Primary
        public JwtUserIdExtractor testJwtUserIdExtractor() {
            return new JwtUserIdExtractor() {
                @Override
                public Long requireUserId(Jwt jwt) {
                    return 1L;
                }
            };
        }

        @Bean
        @Primary
        public BidAccessSecurityUtil testBidAccessSecurityUtil() {
            return new BidAccessSecurityUtil(null, null) {
                @Override
                public boolean isBidOwner(Long bidId) {
                    return true;
                }

                @Override
                public boolean canAccessBid(Long bidId) {
                    return true;
                }

                @Override
                public boolean canAccessTenderer(Long tendererId) {
                    return true;
                }

                @Override
                public Long getCurrentUserId() {
                    return 1L;
                }

                @Override
                public boolean hasPrivilegedRole() {
                    return true;
                }
            };
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private TenderClient tenderClient;

    @MockBean
    private TenderWorkflowGuard tenderWorkflowGuard;

    @MockBean
    private BidEventPublisher bidEventPublisher;

    private TenderDTO mockTender;

    @BeforeEach
    void setUp() {
        mockTender = TenderDTO.builder()
                .id(100L)
                .title("Test Tender")
                .description("A test tender for integration tests")
                .status("PUBLISHED")
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .build();

        when(tenderClient.getTenderById(anyLong())).thenReturn(mockTender);
        when(tenderWorkflowGuard.validateOpenForBidSubmission(anyLong(), any(LocalDateTime.class)))
                .thenReturn(mockTender);
        when(tenderWorkflowGuard.requireTender(anyLong())).thenReturn(mockTender);
        when(tenderWorkflowGuard.getSubmissionDeadline(anyLong()))
                .thenReturn(LocalDateTime.now().plusDays(30));

        doNothing().when(bidEventPublisher).publishBidCreatedEvent(any());
        doNothing().when(bidEventPublisher).publishBidSubmittedEvent(any());
        doNothing().when(bidEventPublisher).publishBidStatusChangedEvent(any(), any());
        doNothing().when(bidEventPublisher).publishBidDeletedEvent(any());
    }

    // ------------------------------------------------------------------ helpers

    private BidSubmissionRequest createValidBidRequest() {
        return BidSubmissionRequest.builder()
                .tenderId(100L)
                .items(List.of(
                        BidItemRequest.builder()
                                .criteriaId(1L)
                                .value(new BigDecimal("1500.00"))
                                .description("Item 1 - Technical proposal")
                                .build(),
                        BidItemRequest.builder()
                                .criteriaId(2L)
                                .value(new BigDecimal("2500.00"))
                                .description("Item 2 - Financial proposal")
                                .build()
                ))
                .build();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Long createBidAndReturnId() {
        BidSubmissionRequest request = createValidBidRequest();
        HttpEntity<BidSubmissionRequest> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<BidDTO> response = restTemplate.postForEntity("/api/bids", entity, BidDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getId();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void testCreateBid() {
        BidSubmissionRequest request = createValidBidRequest();
        HttpEntity<BidSubmissionRequest> entity = new HttpEntity<>(request, jsonHeaders());

        ResponseEntity<BidDTO> response = restTemplate.postForEntity("/api/bids", entity, BidDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        BidDTO bid = response.getBody();
        assertThat(bid.getId()).isNotNull();
        assertThat(bid.getTenderId()).isEqualTo(100L);
        assertThat(bid.getTendererId()).isEqualTo(1L);
        assertThat(bid.getStatus()).isEqualTo(BidStatus.DRAFT);
        assertThat(bid.getTotalPrice()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(bid.getItems()).hasSize(2);
    }

    @Test
    void testGetBidById() {
        Long bidId = createBidAndReturnId();

        ResponseEntity<BidDTO> response = restTemplate.getForEntity("/api/bids/{id}", BidDTO.class, bidId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(bidId);
        assertThat(response.getBody().getStatus()).isEqualTo(BidStatus.DRAFT);
        assertThat(response.getBody().getTenderId()).isEqualTo(100L);
    }

    @Test
    void testSubmitBid() {
        Long bidId = createBidAndReturnId();

        ResponseEntity<BidDTO> response = restTemplate.postForEntity(
                "/api/bids/{id}/submit", null, BidDTO.class, bidId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(BidStatus.SUBMITTED);
        assertThat(response.getBody().getSubmissionTime()).isNotNull();
    }

    @Test
    void testBidStatusTransition() {
        // Create and submit a bid first
        Long bidId = createBidAndReturnId();
        restTemplate.postForEntity("/api/bids/{id}/submit", null, BidDTO.class, bidId);

        // Attempt an invalid transition: SUBMITTED -> DRAFT should fail
        String url = "/api/bids/{id}/status?status=DRAFT";
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.PATCH, null, String.class, bidId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("not allowed");
    }

    @Test
    void testGetBidsByTenderer() {
        // Create a couple of bids
        createBidAndReturnId();
        createBidAndReturnId();

        ResponseEntity<PageDTO> response = restTemplate.getForEntity(
                "/api/bids/tenderer?tendererId=1&page=0&size=10", PageDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(response.getBody().getContent()).isNotNull();
        assertThat(response.getBody().getContent().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testDeleteDraftBid() {
        Long bidId = createBidAndReturnId();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/bids/{id}", HttpMethod.DELETE, null, Void.class, bidId);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify the bid no longer exists
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "/api/bids/{id}", String.class, bidId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
