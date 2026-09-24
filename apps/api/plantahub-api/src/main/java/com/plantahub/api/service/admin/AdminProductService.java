package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.*;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.AdminProductSummaryDTO;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.CreateProductRequest;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.UpdateProductRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

@Service
public class AdminProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ProductMediaRepository mediaRepo;
    private final ProductPlanTypeRepository offerRepo;
    private final OrderRepository orderRepo;
    private final DownloadEntitlementRepository entitlementRepo;

    public AdminProductService(ProductRepository productRepo,
                               CategoryRepository categoryRepo,
                               ProductMediaRepository mediaRepo,
                               ProductPlanTypeRepository offerRepo,
                               OrderRepository orderRepo,
                               DownloadEntitlementRepository entitlementRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.mediaRepo = mediaRepo;
        this.offerRepo = offerRepo;
        this.orderRepo = orderRepo;
        this.entitlementRepo = entitlementRepo;
    }

    @Transactional(readOnly = true)
    public List<Product> list(ProductStatus status, String category, String query) {
        return productRepo.search(status, blankToNull(category), blankToNull(query));
    }

    /**
     * A listagem do painel, ja com o preco derivado das ofertas.
     *
     * <p>Monta o DTO aqui dentro, e nao no controller, pelo mesmo motivo de
     * {@code AdminOfferService.list}: fora da transacao o mapeamento nao teria como
     * consultar nada.
     */
    @Transactional(readOnly = true)
    public List<AdminProductSummaryDTO> listSummaries(ProductStatus status, String category, String query) {
        List<Product> products = list(status, category, query);

        Map<String, Integer> prices = new HashMap<>();

        if (!products.isEmpty()) {
            var ids = products.stream().map(Product::getId).toList();

            for (Object[] row : offerRepo.findMinPurchasablePriceByProductIds(ids)) {
                prices.put((String) row[0], (Integer) row[1]);
            }
        }

        return products.stream()
                .map(product -> AdminProductSummaryDTO.from(product, prices.get(product.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Product get(String id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("product_not_found"));
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        String category = request.category();

        categoryRepo.findById(category)
                .orElseThrow(() -> new NotFoundException("category_not_found: " + category));

        String slug = slugify(request.slug() != null && !request.slug().isBlank()
                ? request.slug()
                : request.name());

        assertSlugAvailable(category, slug, null);

        String id = request.id() != null && !request.id().isBlank()
                ? request.id().trim()
                : category + "-" + slug;

        if (productRepo.existsById(id)) {
            throw new ConflictException("product_id_taken: " + id);
        }

        Instant now = Instant.now();

        Product product = new Product();
        product.setId(id);
        product.setCategory(category);
        product.setSlug(slug);
        product.setName(request.name());
        product.setShortDesc(request.shortDescription() == null ? "" : request.shortDescription());
        product.setAreaM2(request.areaM2() == null ? 0 : request.areaM2());
        product.setBasePriceCents(request.basePriceCents() == null ? 0 : request.basePriceCents());
        product.setDelivery(request.delivery());
        product.setCustomizable(request.customizable() != null && request.customizable());
        product.setContent(ProductContent.empty());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        // Nasce em rascunho: publicar e sempre uma acao explicita.
        product.applyStatus(ProductStatus.DRAFT);

        return productRepo.save(product);
    }

    @Transactional
    public Product update(String id, UpdateProductRequest request) {
        Product product = get(id);

        if (request.category() != null && !request.category().equals(product.getCategory())) {
            categoryRepo.findById(request.category())
                    .orElseThrow(() -> new NotFoundException("category_not_found: " + request.category()));
            product.setCategory(request.category());
        }

        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = slugify(request.slug());
            assertSlugAvailable(product.getCategory(), slug, product.getId());
            product.setSlug(slug);
        }

        if (request.name() != null) product.setName(request.name());
        if (request.shortDescription() != null) product.setShortDesc(request.shortDescription());
        if (request.areaM2() != null) product.setAreaM2(request.areaM2());
        if (request.basePriceCents() != null) product.setBasePriceCents(request.basePriceCents());
        if (request.delivery() != null) product.setDelivery(request.delivery());
        if (request.customizable() != null) product.setCustomizable(request.customizable());

        product.setUpdatedAt(Instant.now());

        return productRepo.save(product);
    }

    /** Substitui o documento inteiro; o admin salva a pagina de uma vez. */
    @Transactional
    public Product replaceContent(String id, ProductContent content) {
        Product product = get(id);
        product.setContent(content == null ? ProductContent.empty() : content.normalized());
        product.setUpdatedAt(Instant.now());
        return productRepo.save(product);
    }

    @Transactional
    public Product publish(String id) {
        Product product = get(id);

        List<String> problems = publishProblems(product);

        if (!problems.isEmpty()) {
            throw new ConflictException("product_not_publishable", problems);
        }

        product.applyStatus(ProductStatus.PUBLISHED);
        return productRepo.save(product);
    }

    @Transactional
    public Product unpublish(String id) {
        Product product = get(id);
        product.applyStatus(ProductStatus.DRAFT);
        return productRepo.save(product);
    }

    @Transactional
    public Product archive(String id) {
        Product product = get(id);
        product.applyStatus(ProductStatus.ARCHIVED);
        return productRepo.save(product);
    }

    /**
     * Exclusao fisica, permitida apenas enquanto o produto nunca foi vendido.
     *
     * <p>Depois de existir um pedido, apagar quebraria historico e direitos de download. O
     * caminho correto passa a ser arquivar, que tira de venda sem destruir nada.
     */
    @Transactional
    public void delete(String id) {
        Product product = get(id);

        if (orderRepo.existsByItemsProductId(id) || entitlementRepo.existsByProduct_Id(id)) {
            throw new ConflictException("product_has_sales_use_archive");
        }

        productRepo.delete(product);
    }

    /**
     * Tudo que impede a publicacao, de uma vez.
     *
     * <p>Devolver a lista completa em vez do primeiro erro evita que o admin descubra os
     * requisitos um por tentativa.
     */
    @Transactional(readOnly = true)
    public List<String> publishProblems(Product product) {
        List<String> problems = new ArrayList<>();

        if (!mediaRepo.existsByProduct_IdAndRoleAndDeletedAtIsNull(product.getId(), ProductMedia.Role.HERO)) {
            problems.add("missing_hero_image");
        }

        boolean hasPricedOffer = offerRepo.findPurchasableByProductIdWithPlanType(product.getId())
                .stream()
                .anyMatch(offer -> offer.getPriceCents() != null && offer.getPriceCents() > 0);

        if (!hasPricedOffer) {
            problems.add("missing_priced_offer");
        }

        ProductContent content = product.getContent();
        if (content == null || content.headline() == null || content.headline().isBlank()) {
            problems.add("missing_content_headline");
        }

        productRepo.findByCategoryAndSlug(product.getCategory(), product.getSlug())
                .filter(other -> !other.getId().equals(product.getId()))
                .ifPresent(other -> problems.add("slug_taken_by:" + other.getId()));

        return problems;
    }

    // ------------------------------------------------------------------

    private void assertSlugAvailable(String category, String slug, String currentProductId) {
        productRepo.findByCategoryAndSlug(category, slug)
                .filter(other -> !other.getId().equals(currentProductId))
                .ifPresent(other -> {
                    // Arquivar nao libera o slug: a restricao de unicidade e total, e mudar
                    // o slug de um produto arquivado quebraria URLs e SEO ja existentes.
                    String reason = other.getStatus() == ProductStatus.ARCHIVED
                            ? "slug_taken_by_archived_product"
                            : "slug_taken";
                    throw new ConflictException(reason + ": " + other.getId());
                });
    }

    /** Normaliza para o formato de slug: sem acento, minusculo, separado por hifen. */
    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
