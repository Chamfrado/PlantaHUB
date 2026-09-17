package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.Category;
import com.plantahub.api.repository.CategoryRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CreateCategoryRequest;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.UpdateCategoryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AdminCategoryService {

    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z][a-z0-9-]{1,39}$");

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    public AdminCategoryService(CategoryRepository categoryRepo, ProductRepository productRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
    }

    @Transactional(readOnly = true)
    public List<Category> list() {
        return categoryRepo.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Category get(String slug) {
        return categoryRepo.findById(slug)
                .orElseThrow(() -> new NotFoundException("category_not_found"));
    }

    @Transactional(readOnly = true)
    public long productCount(String slug) {
        return productRepo.countByCategory(slug);
    }

    @Transactional
    public Category create(CreateCategoryRequest request) {
        String slug = AdminProductService.slugify(request.slug());

        if (!VALID_SLUG.matcher(slug).matches()) {
            throw new ConflictException("category_slug_invalid: " + request.slug());
        }

        if (categoryRepo.existsById(slug)) {
            throw new ConflictException("category_slug_taken: " + slug);
        }

        return categoryRepo.save(Category.builder()
                .slug(slug)
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .featuredOnHome(request.featuredOnHome() != null && request.featuredOnHome())
                .homeOrder(request.homeOrder() == null ? 0 : request.homeOrder())
                .active(true)
                .build());
    }

    /**
     * Atualiza a categoria. O slug e imutavel: ele e chave estrangeira em {@code product} e
     * aparece nas URLs publicas dos produtos.
     */
    @Transactional
    public Category update(String slug, UpdateCategoryRequest request) {
        Category category = get(slug);

        if (request.name() != null) category.setName(request.name());
        if (request.description() != null) category.setDescription(request.description());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());
        if (request.featuredOnHome() != null) category.setFeaturedOnHome(request.featuredOnHome());
        if (request.homeOrder() != null) category.setHomeOrder(request.homeOrder());
        if (request.active() != null) category.setActive(request.active());

        return categoryRepo.save(category);
    }

    @Transactional
    public void delete(String slug) {
        Category category = get(slug);

        if (productCount(slug) > 0) {
            // A chave estrangeira ja barraria; a mensagem explicita evita que o admin
            // receba apenas "violacao de integridade".
            throw new ConflictException("category_has_products_deactivate_instead");
        }

        categoryRepo.delete(category);
    }
}
