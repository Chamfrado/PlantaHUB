package com.plantahub.api.web;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.repository.PasswordResetCodeRepository;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryNotifications;
import com.plantahub.api.support.TestDataFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PasswordResetFlowTest extends AbstractApiTest {

    private static final String OLD_PASSWORD = "senha-antiga-123";
    private static final String NEW_PASSWORD = "senha-nova-456";

    @Autowired private MockMvc mockMvc;
    @Autowired private InMemoryNotifications notifications;
    @Autowired private AppUserRepository users;
    @Autowired private PasswordResetCodeRepository codes;
    @Autowired private TransactionTemplate tx;

    // ------------------------------------------------------------------ helpers

    private String newEmail() {
        return TestDataFactory.unique("reset") + "@plantahub.test";
    }

    private void register(String email, String phone) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + OLD_PASSWORD
                                + "\",\"fullName\":\"Maria Teste\",\"phoneNumber\":" + phoneJson + "}"))
                .andExpect(status().isNoContent());
    }

    private ResultActions requestCode(String email, String channel) throws Exception {
        return mockMvc.perform(post("/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"channel\":\"" + channel + "\"}"));
    }

    private ResultActions verify(String email, String code) throws Exception {
        return mockMvc.perform(post("/v1/auth/password-reset/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"));
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resetToken\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenFrom(String email, String code) throws Exception {
        String body = verify(email, code)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"resetToken\":\"([^\"]+)\".*", "$1");
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    // ------------------------------------------------------------------ fluxo feliz

    @Test
    @DisplayName("por e-mail: pedir, validar, trocar e logar com a senha nova")
    void emailHappyPath() throws Exception {
        String email = newEmail();
        register(email, null);

        requestCode(email, "EMAIL").andExpect(status().isAccepted());
        String code = notifications.awaitCode(email, 0);

        confirm(tokenFrom(email, code), NEW_PASSWORD).andExpect(status().isNoContent());

        login(email, OLD_PASSWORD).andExpect(result ->
                assertThat(result.getResponse().getStatus()).isIn(400, 401, 403));
        login(email, NEW_PASSWORD).andExpect(status().isOk());

        notifications.await(email, m -> m.subject() != null && m.subject().startsWith("Sua senha foi alterada"));
    }

    @Test
    @DisplayName("por SMS: o codigo vai para o celular do cadastro, em E.164")
    void smsHappyPath() throws Exception {
        String email = newEmail();
        register(email, "(11) 98888-7777");

        requestCode(email, "SMS").andExpect(status().isAccepted());
        String code = notifications.awaitCode("+5511988887777", 0);

        confirm(tokenFrom(email, code), NEW_PASSWORD).andExpect(status().isNoContent());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ nao revela contas

    @Test
    @DisplayName("e-mail sem conta recebe o mesmo 202 e nada e enviado")
    void unknownEmailLooksTheSame() throws Exception {
        String email = newEmail();

        requestCode(email, "EMAIL").andExpect(status().isAccepted());

        Thread.sleep(300);
        assertThat(notifications.to(email)).isEmpty();
    }

    @Test
    @DisplayName("SMS para quem nao tem celular: 202 e nada e enviado")
    void smsWithoutPhoneLooksTheSame() throws Exception {
        String email = newEmail();
        register(email, null);

        requestCode(email, "SMS").andExpect(status().isAccepted());

        Thread.sleep(300);
        assertThat(notifications.to(email)).isEmpty();
    }

    // ------------------------------------------------------------------ protecoes

    @Test
    @DisplayName("cinco codigos errados bloqueiam o codigo, mesmo que o certo venha depois")
    void tooManyWrongAttemptsLocksTheCode() throws Exception {
        String email = newEmail();
        register(email, null);
        requestCode(email, "EMAIL").andExpect(status().isAccepted());
        String code = notifications.awaitCode(email, 0);

        for (int i = 0; i < 5; i++) {
            verify(email, wrongCode(code))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_or_expired_code"));
        }

        verify(email, code).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("codigo expirado nao vale")
    void expiredCodeIsRejected() throws Exception {
        String email = newEmail();
        register(email, null);
        requestCode(email, "EMAIL").andExpect(status().isAccepted());
        String code = notifications.awaitCode(email, 0);

        AppUser user = users.findByEmail(email).orElseThrow();
        tx.executeWithoutResult(s -> codes
                .findFirstByUserIdAndVerifiedAtIsNullAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(user.getId())
                .ifPresent(c -> c.setExpiresAt(Instant.now().minusSeconds(1))));

        verify(email, code).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o token de troca so pode ser usado uma vez")
    void resetTokenIsSingleUse() throws Exception {
        String email = newEmail();
        register(email, null);
        requestCode(email, "EMAIL").andExpect(status().isAccepted());
        String token = tokenFrom(email, notifications.awaitCode(email, 0));

        confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());
        confirm(token, "outra-senha-789")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_or_expired_token"));
    }

    @Test
    @DisplayName("segundo pedido em menos de 60s recebe 429, exista a conta ou nao")
    void cooldownPerEmail() throws Exception {
        String existing = newEmail();
        register(existing, null);
        String missing = newEmail();

        requestCode(existing, "EMAIL").andExpect(status().isAccepted());
        requestCode(existing, "EMAIL")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("too_many_requests"));

        requestCode(missing, "EMAIL").andExpect(status().isAccepted());
        requestCode(missing, "EMAIL").andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("trocar a senha derruba as sessoes abertas antes da troca")
    void passwordChangeRevokesOldSessions() throws Exception {
        String email = newEmail();
        register(email, null);

        Cookie oldSession = login(email, OLD_PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
        assertThat(oldSession).isNotNull();
        mockMvc.perform(get("/v1/auth/me").cookie(oldSession)).andExpect(status().isOk());

        // O iat do JWT tem precisao de segundos: sem esta pausa, token e troca cairiam no
        // mesmo segundo e o token (corretamente) ainda seria aceito.
        Thread.sleep(1100);

        requestCode(email, "EMAIL").andExpect(status().isAccepted());
        confirm(tokenFrom(email, notifications.awaitCode(email, 0)), NEW_PASSWORD)
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/auth/me").cookie(oldSession))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        Cookie newSession = login(email, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
        mockMvc.perform(get("/v1/auth/me").cookie(newSession)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ cadastro

    @Test
    @DisplayName("o cadastro aceita celular opcional e guarda so os digitos")
    void registerWithOptionalPhone() throws Exception {
        String withPhone = newEmail();
        register(withPhone, "(21) 3333-4444");
        assertThat(users.findByEmail(withPhone).orElseThrow().getPhoneNumber()).isEqualTo("2133334444");

        String withoutPhone = newEmail();
        register(withoutPhone, null);
        assertThat(users.findByEmail(withoutPhone).orElseThrow().getPhoneNumber()).isNull();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail() + "\",\"password\":\"" + OLD_PASSWORD
                                + "\",\"phoneNumber\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("phone_invalid"));
    }

    @Test
    @DisplayName("os canais disponiveis sao publicos")
    void channelsArePublic() throws Exception {
        mockMvc.perform(get("/v1/auth/password-reset/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(true))
                .andExpect(jsonPath("$.sms").value(true));
    }
}
