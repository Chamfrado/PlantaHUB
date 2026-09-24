package com.plantahub.api.domain.site;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O texto de uma pagina institucional: Sobre, Contato, FAQ, Trabalhe Conosco, Termos,
 * Privacidade.
 *
 * <p><b>Um formato so para todas.</b> Um record por pagina daria cinco arvores de
 * validacao, cinco formularios e cinco telas para aprender, e as paginas sao a mesma coisa
 * por baixo: um cabecalho, secoes de prosa com listas, perguntas e respostas, e uma
 * chamada final. O que as diferencia e o layout — e esse continua sendo de cada pagina, no
 * componente dela, escolhendo quais partes renderizar.
 *
 * <p>Isso e deliberadamente diferente de um construtor de blocos generico: o admin edita
 * texto, e nao monta a pagina. As seis paginas mantem o desenho proprio, que foi o motivo
 * de nao transformar tudo em blocos.
 *
 * <p>Mesmo padrao de {@code ProductContent}: {@code jsonb} numa coluna, com Bean Validation
 * na arvore de records, e substituicao do documento inteiro em vez de mutacao de campos.
 */
public record SitePageContent(

        @Size(max = 200) String headline,
        @Size(max = 300) String subheadline,
        @Size(max = 4000) String intro,

        @Valid @Size(max = 40) List<Section> sections,
        @Valid @Size(max = 60) List<FaqItem> faq,

        @Size(max = 200) String ctaTitle,
        @Size(max = 600) String ctaSubtitle,
        @Size(max = 80) String ctaLabel,
        @Size(max = 500) String ctaHref
) {

    /**
     * Um bloco de texto.
     *
     * <p>{@code items} serve as listas de marcadores que varias paginas usam. Fica separado
     * do corpo porque a pagina precisa saber o que e paragrafo e o que e lista para
     * renderizar cada um do seu jeito — juntar tudo num campo so obrigaria a interpretar
     * marcacao no meio do texto.
     */
    public record Section(
            /**
             * Nome opcional do bloco, para paginas com layout proprio.
             *
             * <p>Termos e Privacidade sao prosa corrida e renderizam as secoes na ordem;
             * Sobre tem blocos com desenho especifico — missao, visao, valores, pilares —
             * e precisa pegar cada um pelo nome. Depender da posicao faria reordenar no
             * painel trocar o conteudo de lugar na tela.
             */
            @Size(max = 60) String key,
            @Size(max = 200) String title,
            @Size(max = 8000) String body,
            @Valid @Size(max = 30) List<Item> items
    ) {}

    /**
     * Um item de lista.
     *
     * <p>{@code title} nulo e um marcador simples, como nas paginas legais; preenchido, e um
     * cartao com titulo e descricao, como os pilares e os valores do Sobre. Um unico campo
     * de texto obrigaria a juntar as duas coisas numa frase so, e a pagina perderia a
     * distincao que o desenho dela usa.
     */
    public record Item(
            @Size(max = 200) String title,
            @Size(max = 800) String text,
            /**
             * Marcadores dentro do proprio item.
             *
             * <p>E o ultimo nivel de aninhamento, e existe porque os cartoes de pilar e de
             * valores do Sobre tem uma lista curta cada um. Achatar essa lista no texto
             * perderia o desenho que a pagina usa; um nivel a mais resolve sem transformar
             * o formato num construtor de blocos.
             */
            @Size(max = 12) List<@Size(max = 300) String> bullets
    ) {
        public Item {
            bullets = bullets == null ? List.of() : List.copyOf(bullets);
        }
    }

    public record FaqItem(
            @Size(max = 60) String category,
            @NotBlank @Size(max = 300) String question,
            @NotBlank @Size(max = 4000) String answer
    ) {}

    /** Nunca devolve nulo em lista: quem renderiza nao precisa se defender de ausencia. */
    public SitePageContent {
        sections = sections == null
                ? List.of()
                : sections.stream()
                        .map(section -> new Section(
                                section.key(), section.title(), section.body(),
                                section.items() == null ? List.of() : List.copyOf(section.items())))
                        .toList();
        faq = faq == null ? List.of() : List.copyOf(faq);
    }

    public static SitePageContent empty() {
        return new SitePageContent(null, null, null, List.of(), List.of(), null, null, null, null);
    }
}
