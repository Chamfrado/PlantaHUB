package com.plantahub.api.config;

import com.plantahub.api.domain.auth.enums.UserRole;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A conta de administrador do ambiente local.
 *
 * <p>O teste mais importante aqui é o primeiro, e ele não sobe contexto nenhum: garante que
 * o componente está fora do perfil {@code prod}. Tudo o mais que este runner faz — criar
 * conta a partir de configuração, com senha abaixo do mínimo que o cadastro exige — só é
 * aceitável porque em produção ele não existe.
 */
class DevAdminAccountRunnerTest extends AbstractApiTest {

    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder encoder;

    private DevAdminAccountRunner runner(String email, String password) {
        return new DevAdminAccountRunner(userRepository, encoder, email, password, "Super Admin");
    }

    private void run(DevAdminAccountRunner runner) {
        runner.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("o runner nao pode existir em producao")
    void excludedFromProdProfile() {
        Profile profile = DevAdminAccountRunner.class.getAnnotation(Profile.class);

        assertThat(profile)
                .as("sem @Profile, uma conta com senha de configuracao nasceria em producao")
                .isNotNull();

        assertThat(profile.value()).containsExactly("!prod");
    }

    @Test
    @DisplayName("cria a conta como ADMIN, com a senha utilizavel no login")
    void createsAdminAccount() {
        String email = TestDataFactory.unique("dev") + "@plantahub.test";

        run(runner(email, "123456"));

        var user = userRepository.findByEmail(email).orElseThrow();

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.isActive()).isTrue();

        assertThat(encoder.matches("123456", user.getPasswordHash()))
                .as("a senha precisa passar pelo mesmo encoder do login, nao ser gravada crua")
                .isTrue();

        assertThat(user.getPasswordHash())
                .as("nem por acidente em texto claro")
                .isNotEqualTo("123456");
    }

    @Test
    @DisplayName("aceita senha menor que o minimo do cadastro pela API")
    void allowsShortPasswordThatTheApiWouldReject() {
        String email = TestDataFactory.unique("dev") + "@plantahub.test";

        // RegisterRequest exige 8 caracteres. Este caminho monta a entidade direto, entao
        // aceita 6 — e e exatamente por isso que ele nao pode existir em producao.
        run(runner(email, "123456"));

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("sem configuracao, nao cria conta nenhuma")
    void doesNothingWithoutConfiguration() {
        long before = userRepository.count();

        run(runner("", ""));
        run(runner("alguem@plantahub.test", ""));
        run(runner("", "123456"));

        assertThat(userRepository.count())
                .as("subir em desenvolvimento nao pode criar conta por acidente")
                .isEqualTo(before);
    }

    @Test
    @DisplayName("promove conta existente sem trocar a senha dela")
    void promotesWithoutResettingPassword() {
        var existing = userRepository.save(com.plantahub.api.domain.auth.AppUser.builder()
                .email(TestDataFactory.unique("user") + "@plantahub.test")
                .passwordHash(encoder.encode("a-senha-original"))
                .fullName("Alguem")
                .role(UserRole.USER)
                .createdAt(java.time.Instant.now())
                .active(true)
                .build());

        run(runner(existing.getEmail(), "outra-senha-qualquer"));

        var reloaded = userRepository.findByEmail(existing.getEmail()).orElseThrow();

        assertThat(reloaded.getRole()).isEqualTo(UserRole.ADMIN);

        assertThat(encoder.matches("a-senha-original", reloaded.getPasswordHash()))
                .as("trocar a senha de uma conta existente no boot seria um sequestro silencioso")
                .isTrue();
    }

    @Test
    @DisplayName("rodar duas vezes nao duplica nem muda nada")
    void isIdempotent() {
        String email = TestDataFactory.unique("dev") + "@plantahub.test";

        run(runner(email, "123456"));
        String hash = userRepository.findByEmail(email).orElseThrow().getPasswordHash();

        run(runner(email, "123456"));

        assertThat(userRepository.findByEmail(email).orElseThrow().getPasswordHash())
                .as("reiniciar a aplicacao nao pode invalidar a sessao de quem esta usando")
                .isEqualTo(hash);
    }
}
