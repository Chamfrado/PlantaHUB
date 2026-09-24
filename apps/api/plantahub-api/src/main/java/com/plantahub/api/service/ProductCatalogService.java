package com.plantahub.api.service;

import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.domain.catalog.Category;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.CategoryRepository;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.web.dto.catalog.PlanTypeOptionDTO;
import com.plantahub.api.web.dto.catalog.ProductDetailDTO;
import com.plantahub.api.web.dto.catalog.ProductSummaryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;
    private final ProductPlanTypeRepository productPlanTypeRepository;
    private final ProductMediaRepository mediaRepository;
    private final CategoryRepository categoryRepository;
    private final DigitalAssetRepository assetRepository;

    public ProductCatalogService(ProductRepository productRepository,
                                 ProductPlanTypeRepository productPlanTypeRepository,
                                 ProductMediaRepository mediaRepository,
                                 CategoryRepository categoryRepository,
                                 DigitalAssetRepository assetRepository) {
        this.productRepository = productRepository;
        this.productPlanTypeRepository = productPlanTypeRepository;
        this.mediaRepository = mediaRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDTO> listProducts(String category, Integer limit) {
        List<Product> products = category != null && !category.isBlank()
                ? productRepository.findByCategoryAndStatusOrderByNameAsc(category, ProductStatus.PUBLISHED)
                : productRepository.findByStatusOrderByCategoryAscNameAsc(ProductStatus.PUBLISHED);

        if (limit != null && limit > 0 && products.size() > limit) {
            products = products.subList(0, limit);
        }

        Map<String, String> categoryNames = categoryNames();
        Map<String, Integer> prices = minPricesOf(products.stream().map(Product::getId).toList());

        return products.stream().map(p -> assembleSummary(p, categoryNames, prices)).toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailDTO getProduct(String category, String slug) {
        var product = productRepository
                .findByCategoryAndSlugAndStatus(category, slug, ProductStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("product_not_found"));

        return assembleDetail(product);
    }

    @Transactional(readOnly = true)
    public List<PlanTypeOptionDTO> getPlanTypes(String category, String slug) {
        var product = productRepository
                .findByCategoryAndSlugAndStatus(category, slug, ProductStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("product_not_found"));

        return assemblePlanTypes(product);
    }

    // ------------------------------------------------------------------
    // Assemblers
    //
    // Publicos porque a pre-visualizacao do admin usa exatamente estes metodos. A unica
    // diferenca entre as duas telas e a busca: a rota publica filtra por PUBLISHED, o
    // preview nao. Dai para frente, mesmo codigo e mesmos DTOs — que e o que impede o
    // preview de mostrar algo diferente do que o cliente vera.
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ProductDetailDTO assembleDetail(Product product) {
        ProductContent content = contentOf(product);

        List<String> gallery = mediaRepository
                .findByProduct_IdAndDeletedAtIsNullOrderByRoleAscSortOrderAsc(product.getId())
                .stream()
                .filter(m -> m.getRole() == ProductMedia.Role.GALLERY)
                .map(this::mediaUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        return new ProductDetailDTO(
                product.getId(),
                product.getCategory(),
                categoryNames().getOrDefault(product.getCategory(), product.getCategory()),
                product.getSlug(),
                product.getName(),
                product.getShortDesc(),
                product.getAreaM2(),
                product.getHeroImageUrl(),
                gallery,
                product.getDelivery(),
                product.getCustomizable(),
                minPricesOf(List.of(product.getId())).get(product.getId()),
                product.getStatus().name(),
                content.tags(),
                fileFormats(product, content),
                content
        );
    }

    @Transactional(readOnly = true)
    public List<PlanTypeOptionDTO> assemblePlanTypes(Product product) {
        return productPlanTypeRepository.findPurchasableByProductIdWithPlanType(product.getId())
                .stream()
                .map(ppt -> new PlanTypeOptionDTO(
                        ppt.getPlanType().getCode(),
                        ppt.getPlanType().getName(),
                        ppt.getPlanType().getDescription(),
                        ppt.getPriceCents(),
                        ppt.getIncludedInBundle()
                ))
                .toList();
    }

    private ProductSummaryDTO assembleSummary(Product product,
                                              Map<String, String> categoryNames,
                                              Map<String, Integer> prices) {
        ProductContent content = contentOf(product);

        return new ProductSummaryDTO(
                product.getId(),
                product.getCategory(),
                categoryNames.getOrDefault(product.getCategory(), product.getCategory()),
                product.getSlug(),
                product.getName(),
                product.getShortDesc(),
                product.getAreaM2(),
                product.getHeroImageUrl(),
                product.getCustomizable(),
                prices.get(product.getId()),
                content.tags(),
                fileFormats(product, content)
        );
    }

    /**
     * Formatos anunciados na vitrine.
     *
     * <p>Quando o admin nao os declara, sao deduzidos das extensoes dos arquivos que o
     * produto realmente tem — assim a vitrine se mantem sozinha em vez de virar mais um
     * campo que alguem esquece de atualizar.
     */
    private List<String> fileFormats(Product product, ProductContent content) {
        if (content.fileFormats() != null && !content.fileFormats().isEmpty()) {
            return content.fileFormats();
        }

        return assetRepository.findPurchasableFileExtensions(product.getId()).stream()
                .filter(ext -> ext != null && !ext.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();
    }

    private ProductContent contentOf(Product product) {
        return product.getContent() == null
                ? ProductContent.empty()
                : product.getContent().normalized();
    }

    /**
     * Devolve {@code null} — e nao a chave do bucket — quando a linha nao tem URL publica.
     *
     * <p>Chave nao e endereco: o navegador resolveria {@code public/products/...} contra a
     * origem do proprio site e mostraria o icone de imagem quebrada. Nulo e filtrado pelo
     * montador, entao a vitrine simplesmente exibe uma imagem a menos.
     */
    private String mediaUrl(ProductMedia media) {
        return media.getPublicUrl();
    }

    /**
     * Menor preco a venda de cada produto: o "a partir de" da vitrine.
     *
     * <p>Substitui a leitura de {@code product.base_price_cents}. A coluna existia como
     * cache desnormalizado e <b>ninguem a atualizava</b>: o painel define preco por oferta,
     * e ela ficava em zero — a vitrine anunciava R$ 0,00 para produto com cinco ofertas
     * pagas. Derivar faz o preco acompanhar as ofertas sozinho, que e o mesmo raciocinio
     * ja usado em {@code fileFormats}.
     *
     * <p>Produto sem nenhuma oferta a venda fica <b>fora</b> do mapa, e o DTO leva
     * {@code null} — que a vitrine sabe distinguir de "de graca".
     */
    private Map<String, Integer> minPricesOf(Collection<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> prices = new HashMap<>();

        for (Object[] row : productPlanTypeRepository.findMinPurchasablePriceByProductIds(productIds)) {
            prices.put((String) row[0], (Integer) row[1]);
        }

        return prices;
    }

    private Map<String, String> categoryNames() {
        Map<String, String> names = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            names.put(category.getSlug(), category.getName());
        }
        return names;
    }
}
