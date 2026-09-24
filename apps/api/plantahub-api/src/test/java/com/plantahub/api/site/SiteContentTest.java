package com.plantahub.api.site;

import com.plantahub.api.domain.site.SitePageContent;
import com.plantahub.api.domain.site.SiteSettings;
import com.plantahub.api.repository.SitePageRepository;
import com.plantahub.api.service.SiteContentService;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.support.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O texto institucional e os dados de contato.
 *
 * <p>Sem {@code @Transactional}: o valor aqui é que a edição <b>persiste</b>, e um rollback
 * automático esconderia justamente isso. Cada teste desfaz o que fez.
 */
class SiteContentTest extends AbstractApiTest {

    @Autowired private SiteContentService service;
    @Autowired private SitePageRepository pageRepo;

    @Test
    @DisplayName("as seis paginas do site existem, e so elas")
    void theSixPagesExist() {
        assertThat(service.listPages())
                .extracting(page -> page.getSlug())
                .containsExactlyInAnyOrder(
                        "sobre", "contato", "faq", "trabalhe-conosco", "termos", "privacidade");
    }

    @Test
    @DisplayName("pagina inexistente e 404, e nao uma pagina em branco")
    void unknownPageIsNotFound() {
        assertThatThrownBy(() -> service.getPage('n' + "ao-existe"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("site_page_not_found");
    }

    @Test
    @DisplayName("o texto que hoje esta no codigo chegou ao banco")
    void seedCarriedTheCurrentText() {
        var termos = service.getPage("termos").getContent();

        assertThat(termos.headline()).contains("Termos de Serviço");
        assertThat(termos.sections())
                .as("o cutover nao pode perder secao: sao doze no documento atual")
                .hasSize(12);

        assertThat(termos.sections().get(1).items())
                .as("os marcadores continuam separados do corpo, para a pagina saber "
                        + "o que e paragrafo e o que e lista")
                .isNotEmpty();

        assertThat(service.getPage("faq").getContent().faq())
                .hasSize(7)
                .allSatisfy(item -> {
                    assertThat(item.question()).isNotBlank();
                    assertThat(item.answer()).isNotBlank();
                });
    }

    @Test
    @DisplayName("os blocos com desenho proprio sao encontrados pelo nome, nao pela posicao")
    void keyedSectionsSurviveReordering() {
        var sobre = service.getPage("sobre").getContent();

        assertThat(sobre.sections())
                .extracting(SitePageContent.Section::key)
                .contains("manifesto", "pilares", "conformidade");

        var pilares = sobre.sections().stream()
                .filter(section -> "pilares".equals(section.key()))
                .findFirst().orElseThrow();

        assertThat(pilares.items())
                .as("cada pilar tem titulo, texto e marcadores proprios")
                .hasSize(3)
                .allSatisfy(item -> assertThat(item.bullets()).isNotEmpty());
    }

    @Test
    @DisplayName("editar o texto persiste, e a leitura publica ve a mudanca")
    void editingPersists() {
        var original = service.getPage("contato").getContent();

        try {
            var edited = new SitePageContent(
                    "Fale com a gente", null, "Respondemos em um dia util.",
                    List.of(new SitePageContent.Section(
                            "formulario", "Envie uma mensagem", "Sem ligacao, so texto.",
                            List.of(new SitePageContent.Item("Prazo", "Um dia util.", List.of())))),
                    List.of(), null, null, null, null);

            service.replaceContent("contato", "Contato", edited, "admin@plantahub.test");

            var reloaded = service.getPage("contato");

            assertThat(reloaded.getContent().headline()).isEqualTo("Fale com a gente");
            assertThat(reloaded.getContent().sections().get(0).items().get(0).title()).isEqualTo("Prazo");
            assertThat(reloaded.getUpdatedBy())
                    .as("quem editou fica registrado: e a unica pista de quem mudou o texto publico")
                    .isEqualTo("admin@plantahub.test");
        } finally {
            service.replaceContent("contato", "Contato", original, "teste");
        }
    }

    @Test
    @DisplayName("os contatos ficam num lugar so, com o Instagram que o rodape usava")
    void settingsAreASingleSource() {
        var settings = service.getSettings();

        // Rodape e pagina de Contato tinham perfis diferentes cravados. O do rodape era o
        // correto, e e ele que ficou.
        assertThat(settings.instagramUrl()).isEqualTo("https://www.instagram.com/planta_hub/");
        assertThat(settings.email()).isEqualTo("contato@plantahub.com.br");
    }

    @Test
    @DisplayName("telefone e WhatsApp de exemplo nao foram semeados")
    void placeholderContactsWereNotSeeded() {
        var settings = service.getSettings();

        // Estavam no ar como "(xx) xxxxx-xxxx" e wa.me/5500000000000. Campo vazio nao
        // aparece na tela; numero falso atende ninguem.
        assertThat(settings.phone()).isNull();
        assertThat(settings.whatsapp()).isNull();
    }

    @Test
    @DisplayName("editar os contatos persiste")
    void settingsPersist() {
        var original = service.getSettings();

        try {
            service.replaceSettings(
                    new SiteSettings("novo@plantahub.com.br", null, null, "5511988887777",
                            null, null, null, null, null, null),
                    "admin@plantahub.test");

            assertThat(service.getSettings().email()).isEqualTo("novo@plantahub.com.br");
            assertThat(service.getSettings().whatsapp()).isEqualTo("5511988887777");
        } finally {
            service.replaceSettings(original, "teste");
        }
    }

    @Test
    @DisplayName("o titulo e o conteudo sao gravados na mesma transacao")
    void titleAndContentSaveTogether() {
        var page = service.getPage("faq");
        String originalTitle = page.getTitle();
        var originalContent = page.getContent();

        try {
            service.replaceContent("faq", "Dúvidas frequentes", originalContent, "teste");

            // O titulo vinha de uma chamada separada no controller, sobre uma entidade ja
            // desanexada: a alteracao nao chegava ao banco e era descartada em silencio.
            assertThat(pageRepo.findById("faq").orElseThrow().getTitle())
                    .isEqualTo("Dúvidas frequentes");
        } finally {
            service.replaceContent("faq", originalTitle, originalContent, "teste");
        }
    }
}
