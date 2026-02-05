package com.plantahub.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SecureTestController {

    @GetMapping("/v1/secure-test")
    public Map<String, Object> secure(Authentication auth) {
        return Map.of(
                "ok", true,
                "principal", auth.getPrincipal()
        );
    }
}
