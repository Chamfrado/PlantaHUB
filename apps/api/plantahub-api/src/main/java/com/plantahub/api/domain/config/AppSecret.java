package com.plantahub.api.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Um segredo operacional definido pelo painel.
 *
 * <p>{@code valueEncrypted} é sempre texto cifrado. A entidade não sabe decifrar de
 * propósito: quem faz isso é {@code SecretCipher}, e quem pode chamá-lo é só o resolvedor
 * de credenciais — não há caminho de leitura que passe por um controller.
 */
@Entity
@Table(name = "app_secret")
public class AppSecret {

    /** Nome da credencial da S3. Único registro previsto por ora. */
    public static final String S3_CREDENTIALS = "s3.credentials";

    @Id
    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "value_encrypted", nullable = false)
    private String valueEncrypted;

    @Column(name = "hint", length = 40)
    private String hint;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    protected AppSecret() {}

    public AppSecret(String name, String valueEncrypted, String hint, String updatedBy) {
        this.name = name;
        this.valueEncrypted = valueEncrypted;
        this.hint = hint;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void replace(String valueEncrypted, String hint, String updatedBy) {
        this.valueEncrypted = valueEncrypted;
        this.hint = hint;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public String getValueEncrypted() {
        return valueEncrypted;
    }

    public String getHint() {
        return hint;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
