package com.egov.tendering.tender.controller;

import com.egov.tendering.tender.config.JwtUserIdExtractor;
import com.egov.tendering.tender.dal.dto.*;
import com.egov.tendering.tender.dal.model.AllocationStrategy;
import com.egov.tendering.tender.dal.model.TenderStatus;
import com.egov.tendering.tender.dal.model.TenderType;
import com.egov.tendering.tender.service.TenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenderController.class)
class TenderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenderService tenderService;

    @MockBean
    private JwtUserIdExtractor jwtUserIdExtractor;

    @MockBean
    private JwtDecoder jwtDecoder;

    private ObjectMapper objectMapper;
    private TenderDTO sampleTenderDTO;
    private final Long TENDER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleTenderDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .description("Test Description")
                .tendereeId(100L)
                .type(TenderType.OPEN)
                .status(TenderStatus.DRAFT)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .allocationStrategy(AllocationStrategy.SINGLE)
                .minWinners(1)
                .maxWinners(3)
                .cutoffScore(BigDecimal.valueOf(70))
                .isAverageAllocation(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .criteria(Collections.emptyList())
                .items(Collections.emptyList())
                .build();

        when(jwtUserIdExtractor.requireUserId(any())).thenReturn(100L);
    }

    @Test
    @DisplayName("GET /{tenderId} should return 200 with tender data")
    void getTenderById_ReturnsOk() throws Exception {
        // Arrange
        when(tenderService.getTenderById(TENDER_ID)).thenReturn(sampleTenderDTO);

        // Act & Assert
        mockMvc.perform(get("/api/tenders/{tenderId}", TENDER_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TENDER_ID))
                .andExpect(jsonPath("$.title").value("Test Tender"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST / should create tender and return 201")
    void createTender_ReturnsCreated() throws Exception {
        // Arrange
        CreateTenderRequest request = CreateTenderRequest.builder()
                .title("New Tender")
                .description("New Description")
                .type(TenderType.OPEN)
                .submissionDeadline(LocalDateTime.now().plusDays(30))
                .allocationStrategy(AllocationStrategy.SINGLE)
                .minWinners(1)
                .maxWinners(3)
                .criteria(List.of(
                        TenderCriteriaRequest.builder()
                                .name("Price")
                                .type(com.egov.tendering.tender.dal.model.CriteriaType.PRICE)
                                .weight(BigDecimal.valueOf(60))
                                .build()
                ))
                .items(List.of(
                        TenderItemRequest.builder()
                                .criteriaId(1L)
                                .name("Item 1")
                                .quantity(10)
                                .build()
                ))
                .build();

        when(tenderService.createTender(any(CreateTenderRequest.class), anyLong()))
                .thenReturn(sampleTenderDTO);

        // Act & Assert
        mockMvc.perform(post("/api/tenders")
                        .with(jwt().jwt(builder -> builder
                                .claim("userId", 100L)
                                .claim("roles", List.of("ROLE_TENDEREE"))
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TENDER_ID))
                .andExpect(jsonPath("$.title").value("Test Tender"));
    }

    @Test
    @DisplayName("POST /{tenderId}/publish should return 200")
    void publishTender_ReturnsOk() throws Exception {
        // Arrange
        TenderDTO publishedDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .status(TenderStatus.PUBLISHED)
                .build();

        when(tenderService.publishTender(TENDER_ID)).thenReturn(publishedDTO);

        // Act & Assert
        mockMvc.perform(post("/api/tenders/{tenderId}/publish", TENDER_ID)
                        .with(jwt().jwt(builder -> builder
                                .claim("userId", 100L)
                                .claim("roles", List.of("ROLE_ADMIN"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("POST /{tenderId}/close should return 200")
    void closeTender_ReturnsOk() throws Exception {
        // Arrange
        TenderDTO closedDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .status(TenderStatus.CLOSED)
                .build();

        when(tenderService.closeTender(TENDER_ID)).thenReturn(closedDTO);

        // Act & Assert
        mockMvc.perform(post("/api/tenders/{tenderId}/close", TENDER_ID)
                        .with(jwt().jwt(builder -> builder
                                .claim("userId", 100L)
                                .claim("roles", List.of("ROLE_TENDEREE"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("POST /{tenderId}/amend should return 200")
    void amendTender_ReturnsOk() throws Exception {
        // Arrange
        TenderAmendmentRequest request = TenderAmendmentRequest.builder()
                .reason("Extended deadline due to holidays")
                .description("Updated description")
                .newSubmissionDeadline(LocalDateTime.now().plusDays(60))
                .build();

        TenderDTO amendedDTO = TenderDTO.builder()
                .id(TENDER_ID)
                .title("Test Tender")
                .status(TenderStatus.AMENDED)
                .description("Updated description")
                .build();

        when(tenderService.amendTender(eq(TENDER_ID), any(TenderAmendmentRequest.class), anyLong()))
                .thenReturn(amendedDTO);

        // Act & Assert
        mockMvc.perform(post("/api/tenders/{tenderId}/amend", TENDER_ID)
                        .with(jwt().jwt(builder -> builder
                                .claim("userId", 100L)
                                .claim("roles", List.of("ROLE_TENDEREE"))
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AMENDED"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("GET /{tenderId}/amendments should return 200 with amendment list")
    void getTenderAmendments_ReturnsOk() throws Exception {
        // Arrange
        TenderAmendmentDTO amendmentDTO = TenderAmendmentDTO.builder()
                .id(1L)
                .tenderId(TENDER_ID)
                .amendmentNumber(1)
                .reason("Extended deadline")
                .description("Description updated")
                .amendedBy(100L)
                .createdAt(LocalDateTime.now())
                .build();

        when(tenderService.getTenderAmendments(TENDER_ID))
                .thenReturn(List.of(amendmentDTO));

        // Act & Assert
        mockMvc.perform(get("/api/tenders/{tenderId}/amendments", TENDER_ID)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amendmentNumber").value(1))
                .andExpect(jsonPath("$[0].reason").value("Extended deadline"));
    }

    @Test
    @DisplayName("Unauthenticated request should return 401")
    void unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/tenders/{tenderId}", TENDER_ID))
                .andExpect(status().isUnauthorized());
    }
}
