package com.plantahub.api.security;

import com.plantahub.api.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repo;

    public AppUserDetailsService(AppUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = repo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("user_not_found"));

        var authority = new SimpleGrantedAuthority("ROLE_" + u.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPasswordHash(),
                List.of(authority)
        );
    }
}