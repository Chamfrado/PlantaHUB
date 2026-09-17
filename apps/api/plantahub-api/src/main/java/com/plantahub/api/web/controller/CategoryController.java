package com.plantahub.api.web.controller;

import com.plantahub.api.domain.catalog.Category;
import com.plantahub.api.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Categorias do catalogo publico.
 *
 * <p>Existe para que o frontend pare de trazer a lista de categorias, seus rotulos e sua
 * ordem escritos no codigo — hoje ele tem um {@code if (category === 'casas')} para
 * decidir o que exibir.
 */
@RestController
@RequestMapping("/v1/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public record CategoryDTO(
            String slug,
            String name,
            String description,
            int order,
            boolean featuredOnHome,
            int homeOrder
    ) {}

    @GetMapping
    public List<CategoryDTO> list() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(CategoryController::toDto)
                .toList();
    }

    private static CategoryDTO toDto(Category c) {
        return new CategoryDTO(
                c.getSlug(),
                c.getName(),
                c.getDescription(),
                c.getSortOrder() == null ? 0 : c.getSortOrder(),
                Boolean.TRUE.equals(c.getFeaturedOnHome()),
                c.getHomeOrder() == null ? 0 : c.getHomeOrder()
        );
    }
}
