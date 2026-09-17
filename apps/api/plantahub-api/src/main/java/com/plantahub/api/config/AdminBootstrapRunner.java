package com.plantahub.api.config;

import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Promove a ADMIN os usuarios listados em {@code app.admin.bootstrap-emails}.
 *
 * <p>Substitui a promocao por e-mail cravado numa migration (V8), que era o unico
 * caminho para ADMIN no sistema inteiro. Como configuracao, isto nao fica versionado no
 * git, e revogavel mudando a variavel de ambiente e nao exige uma nova migration para
 * trocar de dono.
 *
 * <p><b>So promove, nunca rebaixa.</b> A consequencia a aceitar: rebaixar alguem pela API
 * nao adianta enquanto o e-mail continuar na lista, porque o proximo boot promove de
 * volta. Para rebaixar de verdade, tire da variavel primeiro.
 *
 * <p>Nao cria usuarios. Se o e-mail nao tem conta, o runner apenas avisa — criar conta
 * com senha aqui seria um vetor de acesso silencioso.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AppUserRepository userRepository;
    private final List<String> bootstrapEmails;

    public AdminBootstrapRunner(
            AppUserRepository userRepository,
            @Value("${app.admin.bootstrap-emails:}") String bootstrapEmails
    ) {
        this.userRepository = userRepository;
        this.bootstrapEmails = Arrays.stream(bootstrapEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapEmails.isEmpty()) {
            return;
        }

        for (String email : bootstrapEmails) {
            userRepository.findByEmail(email).ifPresentOrElse(
                    user -> {
                        if (user.getRole() == UserRole.ADMIN) {
                            return;
                        }
                        user.setRole(UserRole.ADMIN);
                        userRepository.save(user);
                        log.info("Admin bootstrap: {} promovido a ADMIN", email);
                    },
                    () -> log.warn(
                            "Admin bootstrap: {} esta em app.admin.bootstrap-emails mas nao tem conta. "
                                    + "Cadastre-se primeiro e reinicie a aplicacao.", email)
            );
        }
    }
}
