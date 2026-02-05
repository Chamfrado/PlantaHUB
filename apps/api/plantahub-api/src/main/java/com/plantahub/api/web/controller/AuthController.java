package com.plantahub.api.web.controller;

import com.plantahub.api.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // TEMP: accepts any email/password to generate token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").toLowerCase();
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email_required"));
        }
        String token = jwtService.generateAccessToken(email);
        return ResponseEntity.ok(Map.of("accessToken", token, "tokenType", "Bearer"));
    }
}
