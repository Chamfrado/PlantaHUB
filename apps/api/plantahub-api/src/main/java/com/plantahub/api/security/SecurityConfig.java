package com.plantahub.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
/*
 * A regra de URL /v1/admin/** (abaixo) e o @PreAuthorize em cada controller admin sao
 * mantidos os DOIS de proposito. A regra de URL e o portao externo, que falha fechado
 * para qualquer endpoint que alguem esqueca de anotar — o erro mais provavel conforme a
 * superficie admin cresce de zero para dezenas de rotas. A anotacao e o portao interno,
 * que sobrevive a uma futura mudanca de path. Nenhum dos dois basta sozinho.
 */
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger", "/swagger/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health", "/health/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/health", "/health/**").permitAll()
                        .requestMatchers("/v1/auth/login", "/v1/auth/register", "/v1/auth/logout").permitAll()
                        .requestMatchers("/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/categories", "/v1/categories/**").permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/webhooks/infinitepay").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
