package com.plantahub.api.admin;

import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.service.admin.AdminAssetService;
import com.plantahub.api.service.admin.AdminCollectionService;
import com.plantahub.api.service.admin.AdminMediaService;
import com.plantahub.api.service.admin.AdminOfferService;
import com.plantahub.api.service.admin.AdminProductService;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.CreateCollectionRequest;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.UpdateCollectionRequest;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.UpdateMediaRequest;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.OfferRequest;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.CreateProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** As regras que impedem o painel de destruir dados ou quebrar compras. */
@Transactional
class AdminCatalogRulesTest extends AbstractApiTest {

    @Autowired private AdminProductService productService;
    @Autowired private AdminCollectionService collectionService;
    @Autowired private AdminOfferService offerService;
    @Autowired private AdminMediaService mediaService;
    @Autowired private AdminAssetService assetService;
    @Autowired private ProductCatalogService catalogService;
    @Autowired private ProductMediaRepository mediaRepo;
    @Autowired private TestDataFactory fixtures;

    private com.plantahub.api.domain.catalog.Product newDraft() {
        return productService.create(new CreateProductRequest(
                TestDataFactory.unique("prod"), "casas", TestDataFactory.unique("slug"),
                "Produto", "Curta", 100, 0, "Entrega", true));
    }

    // ---------------- Colecoes ----------------

    @Test
    @DisplayName("o codigo da colecao nao pode ser renomeado")
    void collectionCodeIsImmutable() {
        var created = collectionService.create(new CreateCollectionRequest(
                TestDataFactory.uniqueCode("COL"), "Colecao", null, true, false, 1));

        assertThatThrownBy(() -> collectionService.update(created.getId(),
                new UpdateCollectionRequest("OUTROCODIGO", null, null, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("collection_code_immutable");
    }

    @Test
    @DisplayName("renomear apenas o rotulo funciona")
    void collectionNameIsEditable() {
        var created = collectionService.create(new CreateCollectionRequest(
                TestDataFactory.uniqueCode("COL"), "Nome antigo", null, true, false, 1));

        var updated = collectionService.update(created.getId(),
                new UpdateCollectionRequest(created.getCode(), "Nome novo", null, null, null, null));

        assertThat(updated.getName()).isEqualTo("Nome novo");
        assertThat(updated.getCode()).isEqualTo(created.getCode());
    }

    @Test
    @DisplayName("colecao em uso nao pode ser apagada")
    void collectionInUseCannotBeDeleted() {
        var product = newDraft();
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 1000);

        assertThatThrownBy(() -> collectionService.delete(collection.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("collection_in_use");
    }

    // ---------------- Ofertas ----------------

    @Test
    @DisplayName("oferta com arquivos nao pode ser removida")
    void offerWithAssetsCannotBeRemoved() {
        var product = newDraft();
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 1000);
        fixtures.asset(offer, "planta.pdf");

        assertThatThrownBy(() -> offerService.remove(product.getId(), collection.getCode()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("offer_has_assets");
    }

    @Test
    @DisplayName("upsert cria a oferta na primeira chamada e atualiza na segunda")
    void offerUpsert() {
        var product = newDraft();
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));

        // O servico devolve DTO, e nao entidade: montar o DTO dentro da transacao e o que
        // impede leitura preguicosa depois que ela fecha.
        var created = offerService.upsert(product.getId(), collection.getCode(),
                new OfferRequest(150000, true, false, 1));
        assertThat(created.priceCents()).isEqualTo(150000);

        var updated = offerService.upsert(product.getId(), collection.getCode(),
                new OfferRequest(200000, null, null, null));
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.priceCents()).isEqualTo(200000);
        assertThat(updated.available()).isTrue();
    }

    // ---------------- Midia ----------------

    @Test
    @DisplayName("promover uma imagem a capa rebaixa a anterior")
    void promotingHeroDemotesPrevious() {
        var product = newDraft();

        var first = mediaRepo.save(ProductMedia.builder()
                .product(product).role(ProductMedia.Role.HERO)
                .storageKey("products/" + product.getId() + "/a.webp")
                .publicUrl("https://cdn.test/a.webp").sortOrder(0).build());

        var second = mediaRepo.save(ProductMedia.builder()
                .product(product).role(ProductMedia.Role.GALLERY)
                .storageKey("products/" + product.getId() + "/b.webp")
                .publicUrl("https://cdn.test/b.webp").sortOrder(1).build());

        mediaService.update(second.getId(), new UpdateMediaRequest(ProductMedia.Role.HERO, null, null));

        assertThat(mediaRepo.findById(first.getId()).orElseThrow().getRole())
                .as("sem rebaixar a anterior, o indice de capa unica recusaria a escrita")
                .isEqualTo(ProductMedia.Role.GALLERY);

        assertThat(productService.get(product.getId()).getHeroImageUrl())
                .as("o cache de leitura em product.hero_image_url precisa acompanhar")
                .isEqualTo("https://cdn.test/b.webp");
    }

    // ---------------- Arquivos ----------------

    @Test
    @DisplayName("mover arquivo troca a colecao sem tocar na chave do bucket")
    void movingAssetKeepsStorageKey() {
        var product = newDraft();

        var from = fixtures.collection(TestDataFactory.uniqueCode("FROM"));
        var to = fixtures.collection(TestDataFactory.uniqueCode("TO"));

        var fromOffer = fixtures.offer(product, from, 1000);
        fixtures.offer(product, to, 1000);

        var asset = fixtures.asset(fromOffer, "planta.pdf");
        String originalKey = asset.getStorageKey();

        var moved = assetService.move(asset.getId(), to.getCode());

        assertThat(moved.getProductPlanType().getPlanType().getCode()).isEqualTo(to.getCode());
        assertThat(moved.getStorageKey())
                .as("mover objetos invalidaria URLs assinadas ja emitidas")
                .isEqualTo(originalKey);
    }

    @Test
    @DisplayName("apagar arquivo ja vendido exige confirmacao explicita")
    void deletingGrantedAssetRequiresForce() {
        var user = fixtures.user();
        var product = newDraft();
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 1000);
        var asset = fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, com.plantahub.api.domain.orders.enums.OrderStatus.PAID,
                product, collection, 1000);
        var entitlement = fixtures.entitlement(user, order, product, collection);

        // Simula a pinagem feita no pagamento.
        fixtures.pin(entitlement, asset);

        assertThatThrownBy(() -> assetService.delete(asset.getId(), false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("asset_granted_to_customers");

        // Forcado: some do catalogo, mas continua valendo para quem ja comprou.
        assetService.delete(asset.getId(), true);
        assertThat(asset.getDeletedAt()).isNotNull();
    }

    // ---------------- Produto ----------------

    @Test
    @DisplayName("produto com vendas nao pode ser apagado, so arquivado")
    void productWithSalesCannotBeDeleted() {
        var user = fixtures.user();
        var product = newDraft();
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 1000);
        fixtures.order(user, com.plantahub.api.domain.orders.enums.OrderStatus.PAID,
                product, collection, 1000);

        assertThatThrownBy(() -> productService.delete(product.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("product_has_sales_use_archive");
    }

    @Test
    @DisplayName("slug repetido na mesma categoria e recusado")
    void duplicateSlugIsRejected() {
        var first = newDraft();

        assertThatThrownBy(() -> productService.create(new CreateProductRequest(
                TestDataFactory.unique("outro"), "casas", first.getSlug(),
                "Outro", "Curta", 100, 0, null, false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("slug_taken");
    }

    @Test
    @DisplayName("arquivar nao libera o slug, e o erro diz o porque")
    void archivedProductKeepsItsSlug() {
        var first = newDraft();
        productService.archive(first.getId());

        assertThatThrownBy(() -> productService.create(new CreateProductRequest(
                TestDataFactory.unique("outro"), "casas", first.getSlug(),
                "Outro", "Curta", 100, 0, null, false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("slug_taken_by_archived_product");
    }

    @Test
    @DisplayName("o preview usa o mesmo montador da pagina publica")
    void previewMatchesPublicAssembler() {
        var product = newDraft();
        productService.replaceContent(product.getId(), new ProductContent(
                "Titulo do preview", null, null,
                null, null, List.of(), null, null, List.of(),
                null, null, List.of(), null, null, List.of(),
                null, null, List.of(), null, null, List.of(), List.of()));

        var preview = catalogService.assembleDetail(productService.get(product.getId()));

        assertThat(preview.status()).isEqualTo(ProductStatus.DRAFT.name());
        assertThat(preview.content().headline()).isEqualTo("Titulo do preview");
        assertThat(preview.categoryName())
                .as("o preview traz o rotulo da categoria igual ao publico")
                .isEqualTo("Casas");

        // A rota publica nao enxerga um rascunho.
        assertThatThrownBy(() -> catalogService.getProduct(product.getCategory(), product.getSlug()))
                .hasMessageContaining("product_not_found");
    }
}
