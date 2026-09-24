package com.plantahub.api.web.controller;

import com.plantahub.api.service.PasswordResetService;
import com.plantahub.api.web.dto.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recuperacao de senha. Tudo publico: quem esta aqui, por definicao, nao consegue logar.
 *
 * <p>O IP vem de {@code getRemoteAddr()}: em producao o nginx repassa o cliente real e o
 * Spring o aplica ({@code forward-headers-strategy: framework}).
 */
@RestController
@RequestMapping("/v1/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @GetMapping("/channels")
    public PasswordResetChannelsResponse channels() {
        return new PasswordResetChannelsResponse(true, service.isSmsAvailable());
    }

    /** Sempre 202, exista a conta ou nao. */
    @PostMapping("/request")
    public ResponseEntity<Void> request(@Valid @RequestBody PasswordResetRequest req, HttpServletRequest http) {
        service.requestCode(req.email(), req.channel(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify")
    public PasswordResetVerifyResponse verify(@Valid @RequestBody PasswordResetVerifyRequest req,
                                              HttpServletRequest http) {
        return new PasswordResetVerifyResponse(service.verifyCode(req.email(), req.code(), http.getRemoteAddr()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody PasswordResetConfirmRequest req) {
        service.confirm(req.resetToken(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
