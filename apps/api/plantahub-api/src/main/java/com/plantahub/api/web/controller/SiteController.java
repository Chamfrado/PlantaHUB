package com.plantahub.api.web.controller;

import com.plantahub.api.domain.site.SiteSettings;
import com.plantahub.api.service.SiteContentService;
import com.plantahub.api.web.dto.site.SiteDTOs.SitePageDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Leitura publica do texto institucional. */
@RestController
@RequestMapping("/v1/site")
public class SiteController {

    private final SiteContentService service;

    public SiteController(SiteContentService service) {
        this.service = service;
    }

    @GetMapping("/pages/{slug}")
    public SitePageDTO page(@PathVariable String slug) {
        return SitePageDTO.from(service.getPage(slug));
    }

    /**
     * Contatos e redes sociais.
     *
     * <p>Endpoint proprio, e nao um campo da pagina de Contato: o rodape precisa dos mesmos
     * dados em toda pagina do site, e carregar a pagina inteira para pegar um link de
     * Instagram seria desperdicio.
     */
    @GetMapping("/settings")
    public SiteSettings settings() {
        return service.getSettings();
    }
}
