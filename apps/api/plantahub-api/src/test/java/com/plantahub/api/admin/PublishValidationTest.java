package com.plantahub.api.admin;

import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.service.admin.AdminProductService;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.CreateProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publicar e a acao com consequencia externa: o produto passa a ser visivel e comprável.
 *
 * <p>A validacao devolve <b>todos</b> os problemas de uma vez. Devolver o primeiro faria o
 * admin descobrir os requisitos por tentativa e erro.
 */
@Transactional
class PublishValidationTest extends AbstractApiTest {

    @Autowired private AdminProductService productService;
    @Autowired private ProductMediaRepository mediaRepo;
    @Autowired private TestDataFactory fixtures;

    private com.plantahub.api.domain.catalog.Product newDraft() {
        return productService.create(new CreateProductRequest(
                TestDataFactory.unique("prod"), "casas", TestDataFactory.unique("slug"),
                "Produto de Teste", "Descricao curta", 100, 0, "Download imediato", true));
    }

    private void giveHero(com.plantahub.api.domain.catalog.Product product) {
        mediaRepo.save(ProductMedia.builder()
                .product(product)
                .role(ProductMedia.Role.HERO)
                .storageKey("products/" + product.getId() + "/cover.webp")
                .publicUrl("https://cdn.test/" + product.getId() + "/cover.webp")
                .sortOrder(0)
                .build());
    }

    private void giveHeadline(com.plantahub.api.domain.catalog.Product product) {
        productService.replaceContent(product.getId(), new ProductContent(
                "Um titulo", null, null,
                null, null, List.of(), null, null, List.of(),
                null, null, List.of(), null, null, List.of(),
                null, null, List.of(), null, null, List.of(), List.of()));
    }

    @Test
    @DisplayName("produto novo nasce em rascunho")
    void createdProductStartsAsDraft() {
        var product = newDraft();

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getActive()).isFalse();
    }

    @Test
    @DisplayName("produto vazio lista todos os problemas de uma vez")
    void reportsEveryProblemAtOnce() {
        var product = newDraft();

        assertThatThrownBy(() -> productService.publish(product.getId()))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(((ConflictException) error).getReasons())
                        .as("o admin precisa ver a lista inteira, nao um erro por tentativa")
                        .contains("missing_hero_image", "missing_priced_offer", "missing_content_headline"));
    }

    @Test
    @DisplayName("oferta com preco zero nao habilita a publicacao")
    void freeOfferIsNotEnough() {
        var product = newDraft();
        giveHero(product);
        giveHeadline(product);

        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 0);

        assertThatThrownBy(() -> productService.publish(product.getId()))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(((ConflictException) error).getReasons())
                        .contains("missing_priced_offer"));
    }

    @Test
    @DisplayName("colecao que so acompanha outras ofertas nao habilita a publicacao")
    void bundledOnlyCollectionIsNotAnOffer() {
        var product = newDraft();
        giveHero(product);
        giveHeadline(product);

        var attachment = fixtures.collection(TestDataFactory.uniqueCode("ATT"), false, true);
        fixtures.offer(product, attachment, 150000);

        assertThatThrownBy(() -> productService.publish(product.getId()))
                .isInstanceOf(ConflictException.class)
                .satisfies(error -> assertThat(((ConflictException) error).getReasons())
                        .as("um anexo com preco nao e uma oferta")
                        .contains("missing_priced_offer"));
    }

    @Test
    @DisplayName("com capa, titulo e oferta paga, publica")
    void publishesWhenComplete() {
        var product = newDraft();
        giveHero(product);
        giveHeadline(product);

        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var published = productService.publish(product.getId());

        assertThat(published.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(published.getActive()).isTrue();
        assertThat(published.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publish-check diz o que falta sem tentar publicar")
    void checkDoesNotPublish() {
        var product = newDraft();

        var problems = productService.publishProblems(product);

        assertThat(problems).isNotEmpty();
        assertThat(productService.get(product.getId()).getStatus()).isEqualTo(ProductStatus.DRAFT);
    }
}
