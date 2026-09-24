package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminCategoryDTOs {

    private AdminCategoryDTOs() {
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 40) String slug,
            @NotBlank @Size(max = 120) String name,
            String description,
            Integer sortOrder,
            Boolean featuredOnHome,
            Integer homeOrder,
            Boolean comingSoon
    ) {}

    public record UpdateCategoryRequest(
            @Size(max = 120) String name,
            String description,
            Integer sortOrder,
            Boolean featuredOnHome,
            Integer homeOrder,
            Boolean comingSoon,
            Boolean active
    ) {}

    /** Ordem completa: a posicao na lista vira o {@code sort_order}. */
    public record ReorderRequest(@NotNull List<String> ids) {}

    public record CategoryProductDTO(String id, String name, String status) {}

    public record CategoryDTO(
            String slug,
            String name,
            String description,
            int sortOrder,
            boolean featuredOnHome,
            int homeOrder,
            boolean comingSoon,
            boolean active,
            long productCount
    ) {
        public static CategoryDTO from(Category c, long productCount) {
            return new CategoryDTO(
                    c.getSlug(), c.getName(), c.getDescription(),
                    c.getSortOrder() == null ? 0 : c.getSortOrder(),
                    Boolean.TRUE.equals(c.getFeaturedOnHome()),
                    c.getHomeOrder() == null ? 0 : c.getHomeOrder(),
                    Boolean.TRUE.equals(c.getComingSoon()),
                    Boolean.TRUE.equals(c.getActive()),
                    productCount
            );
        }
    }
}
