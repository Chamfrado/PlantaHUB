package com.plantahub.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Parametros da recuperacao de senha.
 *
 * @param secret            chave do HMAC que protege codigos e tokens no banco
 * @param emailCodeTtl      validade do codigo enviado por e-mail
 * @param smsCodeTtl        validade do codigo enviado por SMS
 * @param resetTokenTtl     tempo entre validar o codigo e definir a senha nova
 * @param maxAttempts       tentativas erradas antes de o codigo ser bloqueado
 * @param emailCooldown     intervalo minimo entre dois pedidos para o mesmo e-mail
 * @param emailMaxPerHour   pedidos por hora para o mesmo e-mail
 * @param ipMaxRequestsPerHour pedidos de codigo por hora vindos do mesmo IP
 * @param ipMaxVerifyPerHour   tentativas de validacao por hora vindas do mesmo IP
 */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        String secret,
        Duration emailCodeTtl,
        Duration smsCodeTtl,
        Duration resetTokenTtl,
        int maxAttempts,
        Duration emailCooldown,
        int emailMaxPerHour,
        int ipMaxRequestsPerHour,
        int ipMaxVerifyPerHour
) {
}
