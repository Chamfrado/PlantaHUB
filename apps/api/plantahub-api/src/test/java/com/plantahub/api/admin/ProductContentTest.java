package com.plantahub.api.admin;

import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.service.admin.AdminProductService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.AdminProductDTOs.CreateProductRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O conteudo da pagina vive numa coluna {@code jsonb}.
 *
 * <p>A objecao natural a essa escolha e "sem tabelas, sem validacao". Estes testes sao a
 * resposta: a arvore de records leva Bean Validation, e o documento sobrevive intacto ao
 * round-trip pelo banco — ordem das listas e acentuacao inclusive.
 */
@Transactional
class ProductContentTest extends AbstractApiTest {

    @Autowired private AdminProductService productService;

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private ProductContent contentWith(List<ProductContent.Feature> features,
                                       List<ProductContent.Testimonial> testimonials) {
        return new ProductContent(
                "Titulo", "Subtitulo", "Descricao",
                "Por que escolher", "Intro", features,
                "O que inclui", "Intro", List.of(),
                "Fatos", "Intro", List.of(),
                "Depoimentos", "Intro", testimonials,
                "Perguntas", "Intro", List.of(),
                "CTA", "Subtitulo do CTA",
                List.of("tag-a", "tag-b"), List.of("PDF", "DWG"));
    }

    @Test
    @DisplayName("titulo de diferencial em branco e recusado")
    void blankFeatureTitleIsRejected() {
        var content = contentWith(List.of(new ProductContent.Feature("  ", "descricao")), List.of());

        var violations = validator.validate(content);

        assertThat(violations)
                .as("validar a arvore de records e o que substitui as constraints do banco")
                .isNotEmpty();
    }

    @Test
    @DisplayName("lista acima do limite e recusada")
    void oversizedListIsRejected() {
        var tooMany = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> new ProductContent.Feature("Item " + i, null))
                .toList();

        assertThat(validator.validate(contentWith(tooMany, List.of()))).isNotEmpty();
    }

    @Test
    @DisplayName("depoimento sem autor e recusado")
    void testimonialRequiresAuthor() {
        var content = contentWith(List.of(),
                List.of(new ProductContent.Testimonial("Otimo produto", "", null, null)));

        assertThat(validator.validate(content)).isNotEmpty();
    }

    @Test
    @DisplayName("conteudo valido passa")
    void validContentPasses() {
        var content = contentWith(
                List.of(new ProductContent.Feature("Design", "Bem resolvido")),
                List.of(new ProductContent.Testimonial("Excelente", "Maria", null, null)));

        assertThat(validator.validate(content)).isEmpty();
    }

    @Test
    @DisplayName("o documento sobrevive ao banco com ordem e acentos intactos")
    void survivesRoundTrip() {
        var product = productService.create(new CreateProductRequest(
                TestDataFactory.unique("prod"), "casas", TestDataFactory.unique("slug"),
                "Produto", "Curta", 100, 0, null, false));

        var content = new ProductContent(
                "Chalé Prime — 85 m²", "Aconchego", "Descrição com acentuação: ção, ã, ê",
                "Por Quê escolher?", null,
                List.of(new ProductContent.Feature("Primeiro", null),
                        new ProductContent.Feature("Segundo", null),
                        new ProductContent.Feature("Terceiro", null)),
                null, null, List.of(),
                null, null, List.of(new ProductContent.KeyFact("85", "metros quadrados")),
                null, null, List.of(),
                null, null, List.of(new ProductContent.FaqItem("Posso modificar?", "Pode.")),
                null, null,
                List.of("chales", "85m2"), List.of("PDF"));

        productService.replaceContent(product.getId(), content);

        var reloaded = productService.get(product.getId()).getContent();

        assertThat(reloaded.headline()).isEqualTo("Chalé Prime — 85 m²");
        assertThat(reloaded.description()).contains("ção, ã, ê");
        assertThat(reloaded.whyChooseFeatures())
                .as("a ordem das listas e significativa: e a ordem em que a pagina renderiza")
                .extracting(ProductContent.Feature::title)
                .containsExactly("Primeiro", "Segundo", "Terceiro");
        assertThat(reloaded.keyFacts()).hasSize(1);
        assertThat(reloaded.faq().get(0).question()).isEqualTo("Posso modificar?");
        assertThat(reloaded.tags()).containsExactly("chales", "85m2");
    }

    @Test
    @DisplayName("listas nulas viram vazias em vez de vazarem null para a pagina")
    void nullListsAreNormalized() {
        var sparse = new ProductContent(
                "So o titulo", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        var normalized = sparse.normalized();

        assertThat(normalized.whyChooseFeatures()).isEmpty();
        assertThat(normalized.includedItems()).isEmpty();
        assertThat(normalized.testimonials()).isEmpty();
        assertThat(normalized.faq()).isEmpty();
        assertThat(normalized.tags()).isEmpty();
        assertThat(normalized.headline()).isEqualTo("So o titulo");
    }
}
