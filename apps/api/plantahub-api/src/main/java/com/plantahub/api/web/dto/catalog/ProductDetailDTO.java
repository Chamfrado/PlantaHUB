package com.plantahub.api.web.dto.catalog;

import com.plantahub.api.domain.catalog.content.ProductContent;

import java.util.List;

/**
 * O produto inteiro para a pagina publica.
 *
 * <p>Passou a carregar {@code content}: ate agora o texto da pagina vinha de um arquivo
 * TypeScript no frontend, e este record nem sequer tinha campo de preco.
 *
 * <p>O mesmo record serve a rota publica e a pre-visualizacao do admin. Inventar um DTO
 * separado para o preview permitiria que as duas telas divergissem, que e justamente o que
 * uma pre-visualizacao nao pode fazer.
 */
public record ProductDetailDTO(
        String id,
        String category,
        String categoryName,
        String slug,
        String name,
        String shortDescription,
        Integer areaM2,
        String heroImageUrl,
        List<String> galleryImageUrls,
        String delivery,
        Boolean customizable,
        Integer basePriceCents,
        String status,
        List<String> tags,
        List<String> fileFormats,
        ProductContent content
) {}
