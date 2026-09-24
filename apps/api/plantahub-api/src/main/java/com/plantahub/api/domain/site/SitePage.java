package com.plantahub.api.domain.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Uma pagina institucional.
 *
 * <p>O conjunto de paginas e fixo, e a chave primaria e o proprio slug: cada um
 * corresponde a um componente com layout proprio no frontend, entao uma linha sem
 * componente renderizaria em lugar nenhum. Nao ha criacao nem remocao pelo painel.
 */
@Entity
@Table(name = "site_page")
public class SitePage {

    @Id
    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private SitePageContent content = SitePageContent.empty();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private String updatedBy;

    protected SitePage() {}

    /** Substitui o documento inteiro. Mutar campo a campo nao casa com jsonb. */
    public void replaceContent(SitePageContent content, String updatedBy) {
        this.content = content == null ? SitePageContent.empty() : content;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void rename(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public SitePageContent getContent() {
        return content == null ? SitePageContent.empty() : content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
