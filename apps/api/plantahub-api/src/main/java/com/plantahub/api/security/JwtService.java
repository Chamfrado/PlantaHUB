package com.plantahub.api.security;

import com.plantahub.api.domain.auth.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final String issuer;
    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-minutes}") long accessTokenMinutes
    ) {
        this.issuer = issuer;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    // Creates a token for a "subject" (we’ll use email as subject)
    public String generateAccessToken(String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(issuer)
                .subject(email)
                .claim("role", role) // "ADMIN" | "USER"
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    // Validates signature + issuer + expiration and returns subject (email)
    public String validateAndGetSubject(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build();

        Jws<Claims> claims = parser.parseSignedClaims(token);
        return claims.getPayload().getSubject();
    }
}
