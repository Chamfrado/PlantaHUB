package com.plantahub.api.web.dto.site;

import com.plantahub.api.domain.site.SitePage;
import com.plantahub.api.domain.site.SitePageContent;
import com.plantahub.api.domain.site.SiteSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SiteDTOs {

    private SiteDTOs() {}

    public record SitePageDTO(
            String slug,
            String title,
            SitePageContent content,
            Instant updatedAt,
            String updatedBy
    ) {
        public static SitePageDTO from(SitePage page) {
            return new SitePageDTO(
                    page.getSlug(), page.getTitle(), page.getContent(),
                    page.getUpdatedAt(), page.getUpdatedBy());
        }
    }

    /** Resumo para a listagem do painel: sem o conteudo, que e grande e nao cabe numa tabela. */
    public record SitePageSummaryDTO(
            String slug,
            String title,
            Instant updatedAt,
            String updatedBy
    ) {
        public static SitePageSummaryDTO from(SitePage page) {
            return new SitePageSummaryDTO(
                    page.getSlug(), page.getTitle(), page.getUpdatedAt(), page.getUpdatedBy());
        }
    }

    public record UpdatePageRequest(
            @NotBlank @Size(max = 200) String title,
            @Valid SitePageContent content
    ) {}

    public record UpdateSettingsRequest(@Valid SiteSettings settings) {}
}
