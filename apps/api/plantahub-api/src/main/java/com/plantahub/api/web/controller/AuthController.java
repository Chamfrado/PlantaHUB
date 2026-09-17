package com.plantahub.api.web.controller;

import com.plantahub.api.service.AuthService;
import com.plantahub.api.security.AuthCookieService;
import com.plantahub.api.web.dto.auth.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse response
    ) {
        AuthResponse auth = authService.login(req);
        authCookieService.addAccessTokenCookie(response, auth.accessToken());
        // O token sai do corpo de proposito: ele viaja no cookie HttpOnly. O role vai
        // junto para que um admin recem-logado ja saiba disso sem precisar de refresh.
        return ResponseEntity.ok(
                new AuthResponse(null, auth.tokenType(), auth.fullName(), auth.email(), auth.role())
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(authService.me(user.getUsername()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearAccessTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
