package com.plantahub.api.web.controller.admin;

import com.plantahub.api.domain.site.SiteSettings;
import com.plantahub.api.service.SiteContentService;
import com.plantahub.api.web.dto.site.SiteDTOs.SitePageDTO;
import com.plantahub.api.web.dto.site.SiteDTOs.SitePageSummaryDTO;
import com.plantahub.api.web.dto.site.SiteDTOs.UpdatePageRequest;
import com.plantahub.api.web.dto.site.SiteDTOs.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Edicao do texto institucional.
 *
 * <p>So leitura e substituicao: nao ha POST nem DELETE de proposito. Cada slug corresponde
 * a um componente com layout proprio no frontend — criar uma pagina pelo painel produziria
 * uma linha que nao renderiza em lugar nenhum, e apagar uma derrubaria uma rota no ar.
 */
@RestController
@RequestMapping("/v1/admin/site")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteController {

    private final SiteContentService service;

    public AdminSiteController(SiteContentService service) {
        this.service = service;
    }

    @GetMapping("/pages")
    public List<SitePageSummaryDTO> list() {
        return service.listPages().stream().map(SitePageSummaryDTO::from).toList();
    }

    @GetMapping("/pages/{slug}")
    public SitePageDTO get(@PathVariable String slug) {
        return SitePageDTO.from(service.getPage(slug));
    }

    @PutMapping("/pages/{slug}")
    public SitePageDTO update(@PathVariable String slug,
                              @Valid @RequestBody UpdatePageRequest request,
                              @AuthenticationPrincipal UserDetails user) {
        return SitePageDTO.from(service.replaceContent(
                slug, request.title(), request.content(), usernameOf(user)));
    }

    @GetMapping("/settings")
    public SiteSettings settings() {
        return service.getSettings();
    }

    @PutMapping("/settings")
    public SiteSettings updateSettings(@Valid @RequestBody UpdateSettingsRequest request,
                                       @AuthenticationPrincipal UserDetails user) {
        return service.replaceSettings(request.settings(), usernameOf(user));
    }

    private static String usernameOf(UserDetails user) {
        return user == null ? "desconhecido" : user.getUsername();
    }
}
