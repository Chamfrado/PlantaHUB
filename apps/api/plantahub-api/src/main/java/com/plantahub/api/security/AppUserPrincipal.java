package com.plantahub.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/** {@link User} com o instante da ultima troca de senha, para o filtro recusar tokens velhos. */
public class AppUserPrincipal extends User {

    private final Instant passwordChangedAt;

    public AppUserPrincipal(String username, String password,
                            Collection<? extends GrantedAuthority> authorities,
                            Instant passwordChangedAt) {
        super(username, password, authorities);
        this.passwordChangedAt = passwordChangedAt;
    }

    /**
     * O {@code iat} do JWT tem precisao de segundos. A comparacao trunca a troca de senha
     * para o segundo: sem isso, um login feito no mesmo segundo da troca seria recusado.
     */
    public boolean isTokenRevoked(Instant tokenIssuedAt) {
        if (passwordChangedAt == null || tokenIssuedAt == null) return false;
        return tokenIssuedAt.isBefore(passwordChangedAt.truncatedTo(ChronoUnit.SECONDS));
    }
}
