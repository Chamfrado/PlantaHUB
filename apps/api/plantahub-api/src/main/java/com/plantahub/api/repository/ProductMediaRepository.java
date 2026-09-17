package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {

    List<ProductMedia> findByProduct_IdAndDeletedAtIsNullOrderByRoleAscSortOrderAsc(String productId);

    Optional<ProductMedia> findByProduct_IdAndRoleAndDeletedAtIsNull(String productId, ProductMedia.Role role);

    /**
     * Uma imagem qualquer que ja esteja no prefixo publico.
     *
     * <p>Serve de amostra para o diagnostico do bucket: e o unico jeito de checar leitura
     * anonima de verdade, buscando um objeto que existe em vez de interpretar a politica.
     */
    @Query(value = """
        select * from product_media
        where deleted_at is null
          and public_url is not null
          and storage_key like 'public/%'
        limit 1
        """, nativeQuery = true)
    Optional<ProductMedia> findAnyPublicSample();

    boolean existsByProduct_IdAndRoleAndDeletedAtIsNull(String productId, ProductMedia.Role role);
}
