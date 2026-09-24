package com.plantahub.api.domain.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** A linha unica de configuracoes do site. O CHECK no banco garante que so exista uma. */
@Entity
@Table(name = "site_settings")
public class SiteSettingsRow {

    public static final short ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Short id = ID;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private SiteSettings content = SiteSettings.empty();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private String updatedBy;

    protected SiteSettingsRow() {}

    public void replace(SiteSettings content, String updatedBy) {
        this.content = content == null ? SiteSettings.empty() : content;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public SiteSettings getContent() {
        return content == null ? SiteSettings.empty() : content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
