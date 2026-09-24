package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.Category;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.repository.CategoryRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.CreateCategoryRequest;
import com.plantahub.api.web.dto.admin.AdminCategoryDTOs.UpdateCategoryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                // Sem ordem explicita, entra no fim das abas.
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : nextSortOrder())
                .featuredOnHome(request.featuredOnHome() != null && request.featuredOnHome())
                .homeOrder(request.homeOrder() == null ? 0 : request.homeOrder())
                .comingSoon(request.comingSoon() != null && request.comingSoon())
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
        if (request.comingSoon() != null) category.setComingSoon(request.comingSoon());
        if (request.active() != null) category.setActive(request.active());

        return categoryRepo.save(category);
    }

    /** Reordena as abas da pagina de produtos. A posicao na lista vira o sort_order. */
    @Transactional
    public List<Category> reorder(List<String> slugs) {
        for (int i = 0; i < slugs.size(); i++) {
            get(slugs.get(i)).setSortOrder(i + 1);
        }
        return list();
    }

    /** Todos os produtos da categoria, inclusive rascunhos, na ordem da vitrine. */
    @Transactional(readOnly = true)
    public List<Product> products(String slug) {
        get(slug);
        return productRepo.findByCategoryOrderBySortOrderAscNameAsc(slug);
    }

    /** Reordena os produtos dentro da categoria, na vitrine publica. */
    @Transactional
    public List<Product> reorderProducts(String slug, List<String> productIds) {
        Map<String, Product> byId = new HashMap<>();
        products(slug).forEach(p -> byId.put(p.getId(), p));

        for (int i = 0; i < productIds.size(); i++) {
            Product product = byId.get(productIds.get(i));
            if (product == null) {
                // Id de outra categoria aqui e erro do cliente, nao algo a ignorar em silencio.
                throw new ConflictException("product_not_in_category: " + productIds.get(i));
            }
            product.setSortOrder(i + 1);
        }

        return products(slug);
    }

    private int nextSortOrder() {
        return categoryRepo.findAll().stream()
                .mapToInt(c -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                .max().orElse(0) + 1;
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
