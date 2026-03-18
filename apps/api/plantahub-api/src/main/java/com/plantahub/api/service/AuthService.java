package com.plantahub.api.service;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.security.JwtService;
import com.plantahub.api.web.dto.auth.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository repo, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtService jwtService) {
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
                .role(UserRole.USER)   // ✅ correto
                .createdAt(Instant.now())
                .build();

        repo.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().toLowerCase();

        var token = new UsernamePasswordAuthenticationToken(email, req.password());
        authManager.authenticate(token);

        var user = repo.findByEmail(email).orElseThrow(); // já autenticou, então existe

        String jwt = jwtService.generateAccessToken(email, user.getRole().name());
        return new AuthResponse(jwt, "Bearer", user.getFullName());
    }
}
