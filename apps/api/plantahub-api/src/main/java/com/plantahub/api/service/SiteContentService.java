package com.plantahub.api.service;

import com.plantahub.api.domain.site.SitePage;
import com.plantahub.api.domain.site.SitePageContent;
import com.plantahub.api.domain.site.SiteSettings;
import com.plantahub.api.domain.site.SiteSettingsRow;
import com.plantahub.api.repository.SitePageRepository;
import com.plantahub.api.repository.SiteSettingsRepository;
import com.plantahub.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O texto das paginas institucionais e os dados de contato.
 *
 * <p>Leitura e escrita no mesmo servico porque a regra e a mesma dos dois lados, e e curta:
 * o conjunto de paginas e fixo. Nao ha criar nem apagar — cada slug corresponde a um
 * componente com layout proprio, entao uma pagina sem componente renderizaria em lugar
 * nenhum, e apagar uma que tem componente derrubaria uma rota que esta no ar.
 */
@Service
public class SiteContentService {

    private final SitePageRepository pageRepo;
    private final SiteSettingsRepository settingsRepo;

    public SiteContentService(SitePageRepository pageRepo, SiteSettingsRepository settingsRepo) {
        this.pageRepo = pageRepo;
        this.settingsRepo = settingsRepo;
    }

    @Transactional(readOnly = true)
    public List<SitePage> listPages() {
        return pageRepo.findAllByOrderBySlugAsc();
    }

    @Transactional(readOnly = true)
    public SitePage getPage(String slug) {
        return pageRepo.findById(slug)
                .orElseThrow(() -> new NotFoundException("site_page_not_found: " + slug));
    }

    /**
     * Substitui titulo e conteudo numa transacao so.
     *
     * <p>O titulo entra aqui, e nao numa chamada separada do controller: fora da transacao
     * a entidade esta desanexada, entao mutar o titulo la nao chegaria ao banco — e a
     * gravacao do conteudo, logo em seguida, buscaria a linha de novo e descartaria a
     * alteracao em silencio.
     */
    @Transactional
    public SitePage replaceContent(String slug, String title, SitePageContent content, String updatedBy) {
        SitePage page = getPage(slug);

        if (title != null && !title.isBlank()) {
            page.rename(title.trim());
        }

        page.replaceContent(content, updatedBy);
        return pageRepo.save(page);
    }

    @Transactional(readOnly = true)
    public SiteSettings getSettings() {
        return settingsRepo.findById(SiteSettingsRow.ID)
                .map(SiteSettingsRow::getContent)
                .orElseGet(SiteSettings::empty);
    }

    @Transactional
    public SiteSettings replaceSettings(SiteSettings settings, String updatedBy) {
        SiteSettingsRow row = settingsRepo.findById(SiteSettingsRow.ID)
                .orElseThrow(() -> new NotFoundException("site_settings_missing"));

        row.replace(settings, updatedBy);
        settingsRepo.save(row);

        return row.getContent();
    }
}
