package com.egov.tendering.user.security;

import com.egov.tendering.user.dal.model.User;
import com.egov.tendering.user.dal.model.UserRole;
import com.egov.tendering.user.exception.UserNotFoundException;
import com.egov.tendering.user.dal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

        List<GrantedAuthority> authorities = buildAuthorities(user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                authorities
        );
    }

    /**
     * Builds granted authorities from the user's role.
     * Includes the role itself (ROLE_TENDEREE) and all mapped permissions (TENDER_CREATE, BID_READ, etc.)
     * This enables both @PreAuthorize("hasRole('TENDEREE')") and @PreAuthorize("hasAuthority('TENDER_CREATE')")
     */
    private List<GrantedAuthority> buildAuthorities(UserRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add the role (Spring Security expects ROLE_ prefix)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        // Add all permissions for this role
        role.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.name()))
        );

        return authorities;
    }
}
