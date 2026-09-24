package com.plantahub.api.config;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Cria a conta de administrador do ambiente local.
 *
 * <p>Existe porque {@link AdminBootstrapRunner} deliberadamente <b>não</b> cria contas: em
 * produção, uma conta nascendo com senha a partir de configuração é um vetor de acesso
 * silencioso. Aqui a mesma coisa é útil e inofensiva, e as duas afirmações só convivem
 * porque este componente é impossível em produção — {@code @Profile("!prod")}, com teste
 * que trava a anotação no lugar.
 *
 * <p>Três recusas deliberadas:
 *
 * <ul>
 *   <li><b>Não faz nada sem configuração.</b> Subir em desenvolvimento não cria conta
 *       nenhuma por acidente; é preciso informar e-mail e senha.</li>
 *   <li><b>Não troca a senha de conta existente.</b> Quem já tem conta com aquele e-mail
 *       continua com a senha dela; o runner só garante o papel de ADMIN. Resetar senha no
 *       boot seria um jeito silencioso de sequestrar uma conta.</li>
 *   <li><b>Não valida o tamanho mínimo da senha.</b> O cadastro pela API exige 8
 *       caracteres; este caminho monta a entidade direto, então aceita menos. É o que
 *       permite uma senha curta de desenvolvimento — e é também por isso que ele não pode
 *       existir em produção.</li>
 * </ul>
 */
@Component
@Profile("!prod")
public class DevAdminAccountRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminAccountRunner.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder encoder;

    private final String email;
    private final String password;
    private final String fullName;

    public DevAdminAccountRunner(
            AppUserRepository userRepository,
            PasswordEncoder encoder,
            @Value("${app.admin.dev-account.email:}") String email,
            @Value("${app.admin.dev-account.password:}") String password,
            @Value("${app.admin.dev-account.full-name:Super Admin}") String fullName
    ) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }

        var existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            AppUser user = existing.get();

            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                userRepository.save(user);
                log.info("Conta de desenvolvimento {} promovida a ADMIN", email);
            }

            return;
        }

        userRepository.save(AppUser.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .fullName(fullName)
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .active(true)
                .build());

        log.warn("Conta de administrador de DESENVOLVIMENTO criada: {}. "
                + "Ela não existe em produção — o componente que a cria está fora do perfil prod.", email);
    }
}
