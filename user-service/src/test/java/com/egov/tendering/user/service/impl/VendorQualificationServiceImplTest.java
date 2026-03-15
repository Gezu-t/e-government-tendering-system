package com.egov.tendering.user.service.impl;

import com.egov.tendering.user.dal.dto.QualificationReviewRequest;
import com.egov.tendering.user.dal.dto.VendorQualificationDTO;
import com.egov.tendering.user.dal.dto.VendorQualificationRequest;
import com.egov.tendering.user.dal.model.Organization;
import com.egov.tendering.user.dal.model.QualificationStatus;
import com.egov.tendering.user.dal.model.VendorQualification;
import com.egov.tendering.user.dal.repository.OrganizationRepository;
import com.egov.tendering.user.dal.repository.VendorQualificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorQualificationServiceImplTest {

    @Mock
    private VendorQualificationRepository qualificationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private VendorQualificationServiceImpl qualificationService;

    @Captor
    private ArgumentCaptor<VendorQualification> qualificationCaptor;

    private Organization testOrganization;
    private VendorQualification testQualification;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Corp")
                .registrationNumber("REG-001")
                .build();

        testQualification = VendorQualification.builder()
                .id(100L)
                .organizationId(1L)
                .qualificationCategory("IT Services")
                .status(QualificationStatus.PENDING)
                .businessLicenseNumber("BL-12345")
                .taxRegistrationNumber("TAX-67890")
                .yearsOfExperience(5)
                .annualRevenue("1000000")
                .employeeCount(50)
                .pastContractsCount(10)
                .certificationDetails("ISO 9001")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("submitQualification")
    class SubmitQualification {

        @Test
        @DisplayName("should submit qualification successfully")
        void shouldSubmitQualificationSuccessfully() {
            VendorQualificationRequest request = VendorQualificationRequest.builder()
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .businessLicenseNumber("BL-12345")
                    .taxRegistrationNumber("TAX-67890")
                    .yearsOfExperience(5)
                    .annualRevenue("1000000")
                    .employeeCount(50)
                    .pastContractsCount(10)
                    .certificationDetails("ISO 9001")
                    .build();

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(qualificationRepository.findByOrganizationIdAndQualificationCategory(1L, "IT Services"))
                    .thenReturn(Optional.empty());
            when(qualificationRepository.save(any(VendorQualification.class))).thenReturn(testQualification);

            VendorQualificationDTO result = qualificationService.submitQualification(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getOrganizationId()).isEqualTo(1L);
            assertThat(result.getOrganizationName()).isEqualTo("Test Corp");
            assertThat(result.getQualificationCategory()).isEqualTo("IT Services");
            assertThat(result.getStatus()).isEqualTo(QualificationStatus.PENDING);
            assertThat(result.getBusinessLicenseNumber()).isEqualTo("BL-12345");
            assertThat(result.getYearsOfExperience()).isEqualTo(5);

            verify(qualificationRepository).save(qualificationCaptor.capture());
            VendorQualification captured = qualificationCaptor.getValue();
            assertThat(captured.getOrganizationId()).isEqualTo(1L);
            assertThat(captured.getQualificationCategory()).isEqualTo("IT Services");
            assertThat(captured.getStatus()).isEqualTo(QualificationStatus.PENDING);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when organization not found")
        void shouldThrowWhenOrganizationNotFound() {
            VendorQualificationRequest request = VendorQualificationRequest.builder()
                    .organizationId(999L)
                    .qualificationCategory("IT Services")
                    .build();

            when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qualificationService.submitQualification(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Organization not found");

            verify(qualificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when pending qualification exists for same category")
        void shouldThrowWhenDuplicatePendingExists() {
            VendorQualificationRequest request = VendorQualificationRequest.builder()
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .build();

            VendorQualification existingPending = VendorQualification.builder()
                    .id(50L)
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .status(QualificationStatus.PENDING)
                    .build();

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(qualificationRepository.findByOrganizationIdAndQualificationCategory(1L, "IT Services"))
                    .thenReturn(Optional.of(existingPending));

            assertThatThrownBy(() -> qualificationService.submitQualification(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already pending");

            verify(qualificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when under-review qualification exists for same category")
        void shouldThrowWhenDuplicateUnderReviewExists() {
            VendorQualificationRequest request = VendorQualificationRequest.builder()
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .build();

            VendorQualification existingUnderReview = VendorQualification.builder()
                    .id(51L)
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .status(QualificationStatus.UNDER_REVIEW)
                    .build();

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(qualificationRepository.findByOrganizationIdAndQualificationCategory(1L, "IT Services"))
                    .thenReturn(Optional.of(existingUnderReview));

            assertThatThrownBy(() -> qualificationService.submitQualification(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already pending");

            verify(qualificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow resubmission when existing qualification is DISQUALIFIED")
        void shouldAllowResubmissionWhenDisqualified() {
            VendorQualificationRequest request = VendorQualificationRequest.builder()
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .businessLicenseNumber("BL-NEW")
                    .build();

            VendorQualification existingDisqualified = VendorQualification.builder()
                    .id(52L)
                    .organizationId(1L)
                    .qualificationCategory("IT Services")
                    .status(QualificationStatus.DISQUALIFIED)
                    .build();

            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));
            when(qualificationRepository.findByOrganizationIdAndQualificationCategory(1L, "IT Services"))
                    .thenReturn(Optional.of(existingDisqualified));
            when(qualificationRepository.save(any(VendorQualification.class))).thenReturn(testQualification);

            VendorQualificationDTO result = qualificationService.submitQualification(request);

            assertThat(result).isNotNull();
            verify(qualificationRepository).save(any(VendorQualification.class));
        }
    }

    @Nested
    @DisplayName("reviewQualification")
    class ReviewQualification {

        @Test
        @DisplayName("should qualify with valid dates and score")
        void shouldQualifyWithDatesAndScore() {
            LocalDate validFrom = LocalDate.of(2026, 1, 1);
            LocalDate validUntil = LocalDate.of(2027, 1, 1);

            QualificationReviewRequest request = QualificationReviewRequest.builder()
                    .status(QualificationStatus.QUALIFIED)
                    .comments("Meets all requirements")
                    .qualificationScore(85)
                    .validFrom(validFrom)
                    .validUntil(validUntil)
                    .build();

            when(qualificationRepository.findById(100L)).thenReturn(Optional.of(testQualification));
            when(qualificationRepository.save(any(VendorQualification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

            VendorQualificationDTO result = qualificationService.reviewQualification(100L, request, 5L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(QualificationStatus.QUALIFIED);
            assertThat(result.getQualificationScore()).isEqualTo(85);
            assertThat(result.getReviewComments()).isEqualTo("Meets all requirements");
            assertThat(result.getReviewerId()).isEqualTo(5L);
            assertThat(result.getValidFrom()).isEqualTo(validFrom);
            assertThat(result.getValidUntil()).isEqualTo(validUntil);

            verify(qualificationRepository).save(qualificationCaptor.capture());
            VendorQualification captured = qualificationCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(QualificationStatus.QUALIFIED);
            assertThat(captured.getReviewedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set default valid dates when qualifying without explicit dates")
        void shouldSetDefaultDatesWhenQualifyingWithoutDates() {
            QualificationReviewRequest request = QualificationReviewRequest.builder()
                    .status(QualificationStatus.QUALIFIED)
                    .comments("Approved")
                    .build();

            when(qualificationRepository.findById(100L)).thenReturn(Optional.of(testQualification));
            when(qualificationRepository.save(any(VendorQualification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

            qualificationService.reviewQualification(100L, request, 5L);

            verify(qualificationRepository).save(qualificationCaptor.capture());
            VendorQualification captured = qualificationCaptor.getValue();
            assertThat(captured.getValidFrom()).isEqualTo(LocalDate.now());
            assertThat(captured.getValidUntil()).isEqualTo(LocalDate.now().plusYears(1));
        }

        @Test
        @DisplayName("should set default dates for conditionally qualified status")
        void shouldSetDefaultDatesForConditionallyQualified() {
            QualificationReviewRequest request = QualificationReviewRequest.builder()
                    .status(QualificationStatus.CONDITIONALLY_QUALIFIED)
                    .comments("Conditionally approved pending document submission")
                    .build();

            when(qualificationRepository.findById(100L)).thenReturn(Optional.of(testQualification));
            when(qualificationRepository.save(any(VendorQualification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

            VendorQualificationDTO result = qualificationService.reviewQualification(100L, request, 5L);

            assertThat(result.getStatus()).isEqualTo(QualificationStatus.CONDITIONALLY_QUALIFIED);
            verify(qualificationRepository).save(qualificationCaptor.capture());
            VendorQualification captured = qualificationCaptor.getValue();
            assertThat(captured.getValidFrom()).isNotNull();
            assertThat(captured.getValidUntil()).isNotNull();
        }

        @Test
        @DisplayName("should disqualify with rejection reason")
        void shouldDisqualifyWithRejectionReason() {
            QualificationReviewRequest request = QualificationReviewRequest.builder()
                    .status(QualificationStatus.DISQUALIFIED)
                    .comments("Does not meet minimum requirements")
                    .rejectionReason("Insufficient experience and missing certifications")
                    .build();

            when(qualificationRepository.findById(100L)).thenReturn(Optional.of(testQualification));
            when(qualificationRepository.save(any(VendorQualification.class))).thenAnswer(inv -> inv.getArgument(0));
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrganization));

            VendorQualificationDTO result = qualificationService.reviewQualification(100L, request, 5L);

            assertThat(result.getStatus()).isEqualTo(QualificationStatus.DISQUALIFIED);
            assertThat(result.getRejectionReason()).isEqualTo("Insufficient experience and missing certifications");
            assertThat(result.getReviewComments()).isEqualTo("Does not meet minimum requirements");
            assertThat(result.getReviewerId()).isEqualTo(5L);

            verify(qualificationRepository).save(qualificationCaptor.capture());
            VendorQualification captured = qualificationCaptor.getValue();
            assertThat(captured.getRejectionReason())
                    .isEqualTo("Insufficient experience and missing certifications");
            // Disqualified should not have validity dates set by this branch
            assertThat(captured.getValidFrom()).isNull();
            assertThat(captured.getValidUntil()).isNull();
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when qualification not found")
        void shouldThrowWhenQualificationNotFound() {
            QualificationReviewRequest request = QualificationReviewRequest.builder()
                    .status(QualificationStatus.QUALIFIED)
                    .build();

            when(qualificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qualificationService.reviewQualification(999L, request, 5L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Qualification not found");

            verify(qualificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isOrganizationQualified")
    class IsOrganizationQualified {

        @Test
        @DisplayName("should return true when organization is qualified")
        void shouldReturnTrueWhenQualified() {
            when(qualificationRepository.isOrganizationQualified(eq(1L), any(LocalDate.class)))
                    .thenReturn(true);

            boolean result = qualificationService.isOrganizationQualified(1L);

            assertThat(result).isTrue();
            verify(qualificationRepository).isOrganizationQualified(eq(1L), any(LocalDate.class));
        }

        @Test
        @DisplayName("should return false when organization is not qualified")
        void shouldReturnFalseWhenNotQualified() {
            when(qualificationRepository.isOrganizationQualified(eq(2L), any(LocalDate.class)))
                    .thenReturn(false);

            boolean result = qualificationService.isOrganizationQualified(2L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("checkForExpiredQualifications")
    class CheckForExpiredQualifications {

        @Test
        @DisplayName("should mark expired qualifications as EXPIRED")
        void shouldMarkExpiredQualifications() {
            VendorQualification expired1 = VendorQualification.builder()
                    .id(10L)
                    .organizationId(1L)
                    .qualificationCategory("Construction")
                    .status(QualificationStatus.QUALIFIED)
                    .validUntil(LocalDate.now().minusDays(1))
                    .build();

            VendorQualification expired2 = VendorQualification.builder()
                    .id(11L)
                    .organizationId(2L)
                    .qualificationCategory("IT Services")
                    .status(QualificationStatus.QUALIFIED)
                    .validUntil(LocalDate.now().minusMonths(1))
                    .build();

            when(qualificationRepository.findExpiredQualifications(any(LocalDate.class)))
                    .thenReturn(List.of(expired1, expired2));
            when(qualificationRepository.save(any(VendorQualification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            qualificationService.checkForExpiredQualifications();

            verify(qualificationRepository, times(2)).save(qualificationCaptor.capture());
            List<VendorQualification> capturedList = qualificationCaptor.getAllValues();

            assertThat(capturedList).hasSize(2);
            assertThat(capturedList).allSatisfy(q ->
                    assertThat(q.getStatus()).isEqualTo(QualificationStatus.EXPIRED)
            );
            assertThat(capturedList.get(0).getId()).isEqualTo(10L);
            assertThat(capturedList.get(1).getId()).isEqualTo(11L);
        }

        @Test
        @DisplayName("should handle no expired qualifications gracefully")
        void shouldHandleNoExpiredQualifications() {
            when(qualificationRepository.findExpiredQualifications(any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());

            qualificationService.checkForExpiredQualifications();

            verify(qualificationRepository, never()).save(any());
        }
    }
}
