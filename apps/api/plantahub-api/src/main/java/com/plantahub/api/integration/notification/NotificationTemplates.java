package com.plantahub.api.integration.notification;

import org.springframework.web.util.HtmlUtils;

import java.time.Duration;

/**
 * Textos das mensagens, em pt-BR.
 *
 * <p>O SMS vai sem acento de proposito: um unico caractere fora do alfabeto GSM-7 faz a
 * operadora cobrar a mensagem em UCS-2, que cabe 70 caracteres por segmento em vez de 160.
 */
public final class NotificationTemplates {

    private NotificationTemplates() {
    }

    public record Email(String subject, String text, String html) {}

    public static String resetCodeSms(String code, Duration ttl) {
        return "PlantaHub: seu codigo de recuperacao de senha e " + code
                + ". Valido por " + ttl.toMinutes() + " min. Nao compartilhe.";
    }

    public static Email resetCodeEmail(String name, String code, Duration ttl) {
        String greeting = greeting(name);
        String minutes = String.valueOf(ttl.toMinutes());

        String text = greeting + "\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta no PlantaHub.\n\n"
                + "Seu código: " + code + "\n\n"
                + "Ele vale por " + minutes + " minutos. Se você não pediu, ignore este e-mail: "
                + "sua senha continua a mesma.\n";

        String html = layout(
                "<p>" + HtmlUtils.htmlEscape(greeting) + "</p>"
                        + "<p>Recebemos um pedido para redefinir a senha da sua conta no PlantaHub.</p>"
                        + "<p style=\"margin:24px 0;text-align:center\">"
                        + "<span style=\"display:inline-block;padding:14px 24px;border-radius:12px;"
                        + "background:#fff4ea;color:#c25400;font-size:32px;font-weight:700;letter-spacing:8px\">"
                        + code + "</span></p>"
                        + "<p>Ele vale por " + minutes + " minutos.</p>"
                        + "<p style=\"color:#737373;font-size:13px\">Se você não pediu, ignore este e-mail: "
                        + "sua senha continua a mesma.</p>");

        return new Email("Seu código de recuperação de senha - PlantaHub", text, html);
    }

    public static Email passwordChangedEmail(String name) {
        String greeting = greeting(name);

        String text = greeting + "\n\n"
                + "A senha da sua conta no PlantaHub acabou de ser alterada, e as sessões abertas "
                + "foram encerradas.\n\n"
                + "Se não foi você, recupere o acesso agora pela opção \"Esqueci minha senha\" "
                + "e fale com o nosso suporte.\n";

        String html = layout(
                "<p>" + HtmlUtils.htmlEscape(greeting) + "</p>"
                        + "<p>A senha da sua conta no PlantaHub acabou de ser alterada, e as sessões "
                        + "abertas foram encerradas.</p>"
                        + "<p style=\"color:#737373;font-size:13px\">Se não foi você, recupere o acesso "
                        + "agora pela opção \"Esqueci minha senha\" e fale com o nosso suporte.</p>");

        return new Email("Sua senha foi alterada - PlantaHub", text, html);
    }

    private static String greeting(String name) {
        return name == null || name.isBlank() ? "Olá," : "Olá, " + name.trim().split("\\s+")[0] + ",";
    }

    private static String layout(String body) {
        return "<!doctype html><html><body style=\"margin:0;background:#f5f5f5;"
                + "font-family:Arial,Helvetica,sans-serif;color:#262626\">"
                + "<div style=\"max-width:480px;margin:32px auto;background:#ffffff;border-radius:16px;padding:32px\">"
                + "<h1 style=\"margin:0 0 24px;font-size:22px;color:#f77205\">PlantaHub</h1>"
                + body
                + "</div></body></html>";
    }
}
