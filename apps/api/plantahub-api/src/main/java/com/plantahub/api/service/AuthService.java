package com.plantahub.api.service;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.security.JwtService;
import com.plantahub.api.web.dto.auth.AuthResponse;
import com.plantahub.api.web.dto.auth.LoginRequest;
import com.plantahub.api.web.dto.auth.RegisterRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository repo,
                       PasswordEncoder encoder,
                       AuthenticationManager authManager,
                       JwtService jwtService) {
        this.repo = repo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest req) {
        String email = req.email().toLowerCase();

        if (repo.existsByEmail(email)) {
            throw new IllegalArgumentException("email_already_in_use");
        }

        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .fullName(req.fullName())
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .active(true)
                .deletedAt(null)
                .build();

        repo.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.email().toLowerCase();

        var token = new UsernamePasswordAuthenticationToken(email, req.password());
        authManager.authenticate(token);

        var user = repo.findByEmailAndActiveTrueAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("user_deleted"));

        return createAuthResponse(user, issueToken(user));
    }

    public AuthResponse me(String email) {
        var user = repo.findByEmailAndActiveTrueAndDeletedAtIsNull(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return createAuthResponse(user, null);
    }

    public String issueToken(AppUser user) {
        return jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
    }

    public AuthResponse createAuthResponse(AppUser user, String token) {
        return new AuthResponse(token, "Bearer", user.getFullName(), user.getEmail());
    }
}
