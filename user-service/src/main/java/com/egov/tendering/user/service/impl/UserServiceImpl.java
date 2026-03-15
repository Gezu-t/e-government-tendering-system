package com.egov.tendering.user.service.impl;


import com.egov.tendering.user.dal.dto.OrganizationRequest;
import com.egov.tendering.user.dal.dto.RegistrationRequest;
import com.egov.tendering.user.dal.dto.UserDTO;
import com.egov.tendering.user.dal.mapper.UserMapper;
import com.egov.tendering.user.dal.model.*;
import com.egov.tendering.user.dal.repository.UserRepository;
import com.egov.tendering.user.event.UserEventPublisher;
import com.egov.tendering.user.exception.DuplicateResourceException;
import com.egov.tendering.user.exception.UserNotFoundException;
import com.egov.tendering.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import com.egov.tendering.user.dal.repository.OrganizationRepository;
import com.egov.tendering.user.dal.repository.UserOrganizationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDTO registerUser(RegistrationRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        // Create and save user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);

        // If organization details are provided and user is a TENDERER
        if (request.getOrganization() != null && request.getRole() == UserRole.TENDERER) {
            OrganizationRequest orgRequest = request.getOrganization();

            // Check if organization already exists
            if (organizationRepository.existsByRegistrationNumber(orgRequest.getRegistrationNumber())) {
                throw new DuplicateResourceException("Organization with registration number already exists: " +
                        orgRequest.getRegistrationNumber());
            }

            // Create and save organization
            Organization organization = new Organization();
            organization.setName(orgRequest.getName());
            organization.setRegistrationNumber(orgRequest.getRegistrationNumber());
            organization.setAddress(orgRequest.getAddress());
            organization.setContactPerson(orgRequest.getContactPerson());
            organization.setPhone(orgRequest.getPhone());
            organization.setEmail(orgRequest.getEmail());
            organization.setOrganizationType(orgRequest.getOrganizationType());
            organization.setStatus(OrganizationStatus.ACTIVE);

            organization = organizationRepository.save(organization);

            // Create user-organization relationship
            UserOrganization userOrg = new UserOrganization();
            userOrg.setUser(user);
            userOrg.setOrganization(organization);
            userOrg.setRole("ADMIN"); // Default role for organization creator

            userOrganizationRepository.save(userOrg);
        }

        // Load the user with organizations
        User finalUser = user;
        User savedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("" + finalUser.getId()));

        // Publish event for user creation
        eventPublisher.publishUserCreatedEvent(savedUser);

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        log.info("Getting user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toDto(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Username not found: " + username));

        return userMapper.toDto(user);
    }

    @Override
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.info("Getting all users with pagination");

        Page<User> users = userRepository.findAll(pageable);

        return users.map(userMapper::toDto);
    }

    @Override
    public List<UserDTO> getUsersByRole(UserRole role) {
        log.info("Getting users by role: {}", role);

        List<User> users = userRepository.findByRole(role);

        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUserStatus(Long userId, UserStatus status) {
        log.info("Updating user status to {} for user ID: {}", status, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        user = userRepository.save(user);

        // Publish event for user status change
        eventPublisher.publishUserStatusChangedEvent(user, oldStatus);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Publish event before deletion
        eventPublisher.publishUserDeletedEvent(user);

        userRepository.delete(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
