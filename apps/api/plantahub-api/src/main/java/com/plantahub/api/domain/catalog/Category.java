package com.plantahub.api.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Uma categoria do catalogo.
 *
 * <p>E entidade, e nao um valor derivado da lista de produtos, por duas razoes praticas:
 * o admin precisa criar a categoria <b>antes</b> do primeiro produto existir nela, e o
 * rotulo exibido ("Chalés") e a ordem sao dados editoriais que nao se deduzem do slug.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "category")
public class Category {

    @Id
    @Column(name = "slug", length = 40)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Se ganha uma vitrine propria na home. */
    @Column(name = "featured_on_home", nullable = false)
    @Builder.Default
    private Boolean featuredOnHome = false;

    @Column(name = "home_order", nullable = false)
    @Builder.Default
    private Integer homeOrder = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
