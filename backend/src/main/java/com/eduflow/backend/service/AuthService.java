package com.eduflow.backend.service;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.Role;
import com.eduflow.backend.entity.User;
import com.eduflow.backend.exception.DuplicateEmailException;
import com.eduflow.backend.repository.UserRepository;
import com.eduflow.backend.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    @org.springframework.beans.factory.annotation.Autowired private AuditRecorder audit;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
        User user = new User(request.fullName().strip(), email,
                passwordEncoder.encode(request.password()), Role.STUDENT);
        try {
            var saved=users.saveAndFlush(user);audit.record("USER_CREATED","USER",saved.getId(),"Public student account registered");return UserResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint also protects concurrent registrations.
            throw new DuplicateEmailException();
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String email = normalizeEmail(request.email());
        User user=users.findForAuthentication(email).orElse(null);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        // Keep DaoAuthenticationProvider's dummy-password timing protection for unknown accounts.
        if(user==null)throw new BadCredentialsException("Invalid credentials");
        return new AuthResponse(jwtService.generateToken(email,user.getSecurityVersion()), user.getId(), user.getFullName(),
                user.getEmail(), user.getRole());
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        return UserResponse.from(users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
