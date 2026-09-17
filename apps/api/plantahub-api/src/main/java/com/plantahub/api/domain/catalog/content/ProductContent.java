package com.plantahub.api.domain.catalog.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O texto da pagina publica de um produto.
 *
 * <p>Persistido como {@code jsonb} numa unica coluna. A objecao natural a isso e a
 * validacao — resolvida aqui: a arvore de records leva Bean Validation, entao tamanho,
 * obrigatoriedade e limites de cardinalidade sao verificados na entrada como em qualquer
 * DTO, sem custar cinco tabelas e cinco CRUDs por um poder de consulta que nenhuma tela usa.
 *
 * <p>Imutavel de proposito: o admin salva o documento inteiro de uma vez, e o Hibernate
 * detecta mudanca em JSON comparando a serializacao. Substituir o objeto sempre, em vez de
 * mutar campos, casa com as duas coisas.
 */
public record ProductContent(

        @Size(max = 200) String headline,
        @Size(max = 200) String subheadline,
        @Size(max = 4000) String description,

        @Size(max = 200) String whyChooseTitle,
        @Size(max = 1000) String whyChooseIntro,
        @Valid @Size(max = 12) List<Feature> whyChooseFeatures,

        @Size(max = 200) String includesTitle,
        @Size(max = 1000) String includesIntro,
        @Valid @Size(max = 30) List<IncludedItem> includedItems,

        @Size(max = 200) String keyFactsTitle,
        @Size(max = 1000) String keyFactsIntro,
        @Valid @Size(max = 12) List<KeyFact> keyFacts,

        @Size(max = 200) String testimonialsTitle,
        @Size(max = 1000) String testimonialsIntro,
        @Valid @Size(max = 30) List<Testimonial> testimonials,

        @Size(max = 200) String faqTitle,
        @Size(max = 1000) String faqIntro,
        @Valid @Size(max = 30) List<FaqItem> faq,

        @Size(max = 200) String finalCtaTitle,
        @Size(max = 400) String finalCtaSubtitle,

        @Size(max = 20) List<@Size(max = 40) String> tags,

        /** Formatos anunciados na vitrine (BIM, DWG...). Editorial, nao derivado. */
        @Size(max = 10) List<@Size(max = 20) String> fileFormats
) {

    public record Feature(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 2000) String description
    ) {}

    public record IncludedItem(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 2000) String description,
            @Size(max = 500) String imageUrl
    ) {}

    public record KeyFact(
            @NotBlank @Size(max = 20) String value,
            @NotBlank @Size(max = 160) String label
    ) {}

    public record Testimonial(
            @NotBlank @Size(max = 2000) String quote,
            @NotBlank @Size(max = 120) String authorName,
            @Size(max = 120) String authorTitle,
            @Size(max = 500) String avatarUrl
    ) {}

    public record FaqItem(
            @NotBlank @Size(max = 400) String question,
            @Size(max = 4000) String answer
    ) {}

    /** Documento vazio, para produtos recem-criados. */
    public static ProductContent empty() {
        return new ProductContent(
                null, null, null,
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null, List.of(),
                null, null,
                List.of(), List.of()
        );
    }

    /**
     * Listas nulas viram vazias.
     *
     * <p>Normalizar na entrada evita que a checagem {@code != null} se espalhe pelo
     * servico, pelo DTO e pelo componente da pagina.
     */
    public ProductContent normalized() {
        return new ProductContent(
                headline, subheadline, description,
                whyChooseTitle, whyChooseIntro, orEmpty(whyChooseFeatures),
                includesTitle, includesIntro, orEmpty(includedItems),
                keyFactsTitle, keyFactsIntro, orEmpty(keyFacts),
                testimonialsTitle, testimonialsIntro, orEmpty(testimonials),
                faqTitle, faqIntro, orEmpty(faq),
                finalCtaTitle, finalCtaSubtitle,
                orEmpty(tags), orEmpty(fileFormats)
        );
    }

    private static <T> List<T> orEmpty(List<T> value) {
        return value == null ? List.of() : value;
    }
}
