package com.plantahub.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 🔥 IGNORA ROTAS PÚBLICAS
        if (path.equals("/health") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/v1/auth")) {

            chain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 🔥 SEM TOKEN → NÃO BLOQUEIA
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            String email = jwtService.validateAndGetSubject(token);
            UserDetails user = userDetailsService.loadUserByUsername(email);

            var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            // 🔥 NÃO QUEBRA A REQUISIÇÃO
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}