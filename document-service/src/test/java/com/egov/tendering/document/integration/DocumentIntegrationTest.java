package com.egov.tendering.document.integration;

import com.egov.tendering.document.dal.dto.DocumentUploadRequest;
import com.egov.tendering.document.dal.model.DocumentType;
import com.egov.tendering.document.dal.model.EntityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        "app.security.jwt.claim-roles=roles"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("document_test")
            .withUsername("test")
            .withPassword("test");

    /** Temporary directory for document storage during tests */
    static Path storageDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        storageDir = Files.createTempDirectory("document-test-storage");
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("document.storage.location", storageDir::toString);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
                claims.put("roles", List.of("ROLE_ADMIN", "ROLE_TENDEREE"));
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

    private static Long uploadedDocumentId;

    private HttpEntity<MultiValueMap<String, Object>> buildUploadRequest(
            DocumentUploadRequest metadata, byte[] fileBytes, String filename) throws Exception {

        // File part
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() { return filename; }
        };

        // Metadata part (JSON)
        HttpHeaders metaHeaders = new HttpHeaders();
        metaHeaders.setContentType(MediaType.APPLICATION_JSON);
        String metaJson = objectMapper.writeValueAsString(metadata);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, fileHeaders));
        body.add("metadata", new HttpEntity<>(metaJson, metaHeaders));

        HttpHeaders reqHeaders = new HttpHeaders();
        reqHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        reqHeaders.setBearerAuth("test-token");

        return new HttpEntity<>(body, reqHeaders);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-token");
        return headers;
    }

    // ==================== Test 1: Upload Document ====================

    @Test
    @Order(1)
    void testUploadDocument() throws Exception {
        DocumentUploadRequest metadata = DocumentUploadRequest.builder()
                .name("Tender Notice Q1 2026")
                .documentType(DocumentType.TENDER_NOTICE)
                .entityId("1")
                .entityType(EntityType.TENDER)
                .description("Official tender notice for IT procurement")
                .isPublic(true)
                .build();

        byte[] fileBytes = "Tender notice document content".getBytes();
        HttpEntity<MultiValueMap<String, Object>> request =
                buildUploadRequest(metadata, fileBytes, "tender-notice.pdf");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Tender Notice Q1 2026");
        assertThat(response.getBody()).contains("TENDER_NOTICE");

        // Extract the ID from the JSON response
        var node = objectMapper.readTree(response.getBody());
        uploadedDocumentId = node.get("id").asLong();
        assertThat(uploadedDocumentId).isGreaterThan(0);
    }

    // ==================== Test 2: Get Document by ID ====================

    @Test
    @Order(2)
    void testGetDocumentById() {
        assertThat(uploadedDocumentId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents/" + uploadedDocumentId,
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Tender Notice Q1 2026");
        assertThat(response.getBody()).contains("TENDER");
    }

    // ==================== Test 3: List Documents by Entity ====================

    @Test
    @Order(3)
    void testListDocumentsByEntity() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/documents/entity/TENDER/1",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Tender Notice Q1 2026");
    }

    // ==================== Test 4: Download Document ====================

    @Test
    @Order(4)
    void testDownloadDocument() {
        assertThat(uploadedDocumentId).isNotNull();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/documents/" + uploadedDocumentId + "/download",
                HttpMethod.GET, entity, byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(new String(response.getBody())).contains("Tender notice document content");
    }

    // ==================== Test 5: Upload second document and search ====================

    @Test
    @Order(5)
    void testUploadBidDocumentAndSearch() throws Exception {
        DocumentUploadRequest metadata = DocumentUploadRequest.builder()
                .name("Bid Submission Form")
                .documentType(DocumentType.BID_DOCUMENT)
                .entityId("10")
                .entityType(EntityType.BID)
                .description("Bid document for tender #1")
                .isPublic(false)
                .build();

        byte[] fileBytes = "Bid submission content".getBytes();
        HttpEntity<MultiValueMap<String, Object>> request =
                buildUploadRequest(metadata, fileBytes, "bid-submission.pdf");

        ResponseEntity<String> uploadResponse = restTemplate.exchange(
                "/api/documents", HttpMethod.POST, request, String.class);
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // List by bid entity
        HttpEntity<Void> listEntity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/documents/entity/BID/10",
                HttpMethod.GET, listEntity, String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains("Bid Submission Form");
    }
}
