package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.UpdateMediaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Capa e galeria do produto. */
@Service
public class AdminMediaService {

    private final ProductRepository productRepo;
    private final ProductMediaRepository mediaRepo;

    public AdminMediaService(ProductRepository productRepo, ProductMediaRepository mediaRepo) {
        this.productRepo = productRepo;
        this.mediaRepo = mediaRepo;
    }

    @Transactional(readOnly = true)
    public List<ProductMedia> list(String productId) {
        requireProduct(productId);
        return mediaRepo.findByProduct_IdAndDeletedAtIsNullOrderByRoleAscSortOrderAsc(productId);
    }

    @Transactional
    public ProductMedia update(UUID mediaId, UpdateMediaRequest request) {
        ProductMedia media = requireMedia(mediaId);

        if (request.altText() != null) media.setAltText(request.altText());
        if (request.sortOrder() != null) media.setSortOrder(request.sortOrder());

        if (request.role() != null && request.role() != media.getRole()) {
            if (request.role() == ProductMedia.Role.HERO) {
                promoteToHero(media);
            } else {
                media.setRole(ProductMedia.Role.GALLERY);
            }
        }

        ProductMedia saved = mediaRepo.save(media);
        refreshHeroCache(media.getProduct());
        return saved;
    }

    /**
     * Promove uma imagem a capa, rebaixando a anterior na mesma transacao.
     *
     * <p>Sem o rebaixamento, o indice parcial "uma capa viva por produto" recusaria a
     * escrita — e o admin veria um erro de banco em vez da troca funcionar.
     */
    private void promoteToHero(ProductMedia media) {
        String productId = media.getProduct().getId();

        mediaRepo.findByProduct_IdAndRoleAndDeletedAtIsNull(productId, ProductMedia.Role.HERO)
                .filter(current -> !current.getId().equals(media.getId()))
                .ifPresent(current -> {
                    current.setRole(ProductMedia.Role.GALLERY);
                    mediaRepo.saveAndFlush(current);
                });

        media.setRole(ProductMedia.Role.HERO);
    }

    @Transactional
    public void reorder(String productId, List<UUID> mediaIds) {
        requireProduct(productId);

        int order = 0;

        for (UUID mediaId : mediaIds) {
            ProductMedia media = requireMedia(mediaId);

            if (!media.getProduct().getId().equals(productId)) {
                throw new ConflictException("media_belongs_to_another_product");
            }

            media.setSortOrder(order++);
            mediaRepo.save(media);
        }
    }

    /**
     * Exclusao logica. O objeto permanece no bucket de proposito: ele pode estar
     * referenciado por uma pagina em cache ou por um link ja compartilhado.
     */
    @Transactional
    public void delete(UUID mediaId) {
        ProductMedia media = requireMedia(mediaId);
        media.setDeletedAt(Instant.now());
        mediaRepo.save(media);
        refreshHeroCache(media.getProduct());
    }

    /**
     * Mantem {@code product.hero_image_url} em dia.
     *
     * <p>A coluna e cache de leitura: quatro DTOs a consultam. Recalcula-la aqui evita que
     * a vitrine mostre uma capa que nao existe mais.
     */
    private void refreshHeroCache(Product product) {
        String url = mediaRepo
                .findByProduct_IdAndRoleAndDeletedAtIsNull(product.getId(), ProductMedia.Role.HERO)
                .map(ProductMedia::getPublicUrl)
                .orElse(null);

        product.setHeroImageUrl(url);
        product.setUpdatedAt(Instant.now());
        productRepo.save(product);
    }

    private Product requireProduct(String productId) {
        return productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("product_not_found"));
    }

    private ProductMedia requireMedia(UUID mediaId) {
        return mediaRepo.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("media_not_found"));
    }
}
