package com.egov.tendering.user.service.impl;

import com.egov.tendering.user.dal.dto.OrganizationRequest;
import com.egov.tendering.user.dal.dto.RegistrationRequest;
import com.egov.tendering.user.dal.dto.UserDTO;
import com.egov.tendering.user.dal.mapper.UserMapper;
import com.egov.tendering.user.dal.model.*;
import com.egov.tendering.user.dal.repository.OrganizationRepository;
import com.egov.tendering.user.dal.repository.UserOrganizationRepository;
import com.egov.tendering.user.dal.repository.UserRepository;
import com.egov.tendering.user.event.UserEventPublisher;
import com.egov.tendering.user.exception.DuplicateResourceException;
import com.egov.tendering.user.exception.UserNotFoundException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserOrganizationRepository userOrganizationRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Organization> organizationCaptor;

    @Captor
    private ArgumentCaptor<UserOrganization> userOrgCaptor;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(UserRole.TENDERER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .organizations(new HashSet<>())
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.TENDERER)
                .status(UserStatus.ACTIVE)
                .createdAt(testUser.getCreatedAt())
                .build();
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should register user with TENDERER role and create organization")
        void shouldRegisterTendererWithOrganization() {
            // Arrange
            OrganizationRequest orgRequest = OrganizationRequest.builder()
                    .name("Test Corp")
                    .registrationNumber("REG-001")
                    .address("123 Main St")
                    .contactPerson("John Doe")
                    .phone("555-1234")
                    .email("org@test.com")
                    .organizationType(OrganizationType.PRIVATE)
                    .build();

            RegistrationRequest request = RegistrationRequest.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("password123")
                    .role(UserRole.TENDERER)
                    .organization(orgRequest)
                    .build();

            Organization savedOrg = Organization.builder()
                    .id(10L)
                    .name("Test Corp")
                    .registrationNumber("REG-001")
                    .status(OrganizationStatus.ACTIVE)
                    .build();

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(organizationRepository.existsByRegistrationNumber("REG-001")).thenReturn(false);
            when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);
            when(userOrganizationRepository.save(any(UserOrganization.class)))
                    .thenReturn(new UserOrganization());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

            // Act
            UserDTO result = userService.registerUser(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getRole()).isEqualTo(UserRole.TENDERER);

            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getUsername()).isEqualTo("testuser");
            assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(capturedUser.getPasswordHash()).isEqualTo("encodedPassword");
            assertThat(capturedUser.getRole()).isEqualTo(UserRole.TENDERER);
            assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            verify(organizationRepository).save(organizationCaptor.capture());
            Organization capturedOrg = organizationCaptor.getValue();
            assertThat(capturedOrg.getName()).isEqualTo("Test Corp");
            assertThat(capturedOrg.getRegistrationNumber()).isEqualTo("REG-001");
            assertThat(capturedOrg.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);

            verify(userOrganizationRepository).save(userOrgCaptor.capture());
            UserOrganization capturedUserOrg = userOrgCaptor.getValue();
            assertThat(capturedUserOrg.getUser()).isEqualTo(testUser);
            assertThat(capturedUserOrg.getOrganization()).isEqualTo(savedOrg);
            assertThat(capturedUserOrg.getRole()).isEqualTo("ADMIN");

            verify(eventPublisher).publishUserCreatedEvent(testUser);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username already exists")
        void shouldThrowWhenUsernameExists() {
            RegistrationRequest request = RegistrationRequest.builder()
                    .username("existinguser")
                    .email("new@example.com")
                    .password("password123")
                    .role(UserRole.TENDERER)
                    .build();

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void shouldThrowWhenEmailExists() {
            RegistrationRequest request = RegistrationRequest.builder()
                    .username("newuser")
                    .email("existing@example.com")
                    .password("password123")
                    .role(UserRole.TENDERER)
                    .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not create organization for non-TENDERER role")
        void shouldNotCreateOrgForNonTenderer() {
            RegistrationRequest request = RegistrationRequest.builder()
                    .username("evaluator1")
                    .email("eval@example.com")
                    .password("password123")
                    .role(UserRole.EVALUATOR)
                    .organization(OrganizationRequest.builder()
                            .name("Some Org")
                            .registrationNumber("REG-999")
                            .organizationType(OrganizationType.GOVERNMENT)
                            .build())
                    .build();

            User savedUser = User.builder()
                    .id(2L)
                    .username("evaluator1")
                    .email("eval@example.com")
                    .role(UserRole.EVALUATOR)
                    .status(UserStatus.ACTIVE)
                    .organizations(new HashSet<>())
                    .build();

            UserDTO evaluatorDTO = UserDTO.builder()
                    .id(2L)
                    .username("evaluator1")
                    .role(UserRole.EVALUATOR)
                    .build();

            when(userRepository.existsByUsername("evaluator1")).thenReturn(false);
            when(userRepository.existsByEmail("eval@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userRepository.findById(2L)).thenReturn(Optional.of(savedUser));
            when(userMapper.toDto(savedUser)).thenReturn(evaluatorDTO);

            UserDTO result = userService.registerUser(request);

            assertThat(result.getRole()).isEqualTo(UserRole.EVALUATOR);
            verify(organizationRepository, never()).save(any());
            verify(userOrganizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

            UserDTO result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userMapper, never()).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("getUsersByRole")
    class GetUsersByRole {

        @Test
        @DisplayName("should return list of users with the given role")
        void shouldReturnUsersByRole() {
            User user2 = User.builder()
                    .id(2L)
                    .username("tenderer2")
                    .role(UserRole.TENDERER)
                    .status(UserStatus.ACTIVE)
                    .organizations(new HashSet<>())
                    .build();

            UserDTO dto2 = UserDTO.builder()
                    .id(2L)
                    .username("tenderer2")
                    .role(UserRole.TENDERER)
                    .build();

            when(userRepository.findByRole(UserRole.TENDERER)).thenReturn(List.of(testUser, user2));
            when(userMapper.toDto(testUser)).thenReturn(testUserDTO);
            when(userMapper.toDto(user2)).thenReturn(dto2);

            List<UserDTO> result = userService.getUsersByRole(UserRole.TENDERER);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserDTO::getRole)
                    .containsOnly(UserRole.TENDERER);
            verify(userRepository).findByRole(UserRole.TENDERER);
        }

        @Test
        @DisplayName("should return empty list when no users with role")
        void shouldReturnEmptyListWhenNoUsers() {
            when(userRepository.findByRole(UserRole.COMMITTEE)).thenReturn(Collections.emptyList());

            List<UserDTO> result = userService.getUsersByRole(UserRole.COMMITTEE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatus {

        @Test
        @DisplayName("should update user status and publish event")
        void shouldUpdateStatusAndPublishEvent() {
            User updatedUser = User.builder()
                    .id(1L)
                    .username("testuser")
                    .email("test@example.com")
                    .role(UserRole.TENDERER)
                    .status(UserStatus.SUSPENDED)
                    .organizations(new HashSet<>())
                    .build();

            UserDTO updatedDTO = UserDTO.builder()
                    .id(1L)
                    .username("testuser")
                    .status(UserStatus.SUSPENDED)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(updatedUser);
            when(userMapper.toDto(updatedUser)).thenReturn(updatedDTO);

            UserDTO result = userService.updateUserStatus(1L, UserStatus.SUSPENDED);

            assertThat(result.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            verify(eventPublisher).publishUserStatusChangedEvent(any(User.class), eq(UserStatus.ACTIVE));
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserStatus(999L, UserStatus.SUSPENDED))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
            verify(eventPublisher, never()).publishUserStatusChangedEvent(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should delete user and publish event")
        void shouldDeleteUserAndPublishEvent() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.deleteUser(1L);

            verify(eventPublisher).publishUserDeletedEvent(testUser);
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishUserDeletedEvent(any());
            verify(userRepository, never()).delete(any());
        }
    }
}
