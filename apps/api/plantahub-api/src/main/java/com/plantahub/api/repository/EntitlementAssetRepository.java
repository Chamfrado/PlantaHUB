package com.plantahub.api.repository;

import com.plantahub.api.domain.downloads.EntitlementAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EntitlementAssetRepository extends JpaRepository<EntitlementAsset, UUID> {

    List<EntitlementAsset> findByEntitlement_Id(UUID entitlementId);

    boolean existsByEntitlement_Id(UUID entitlementId);

    /** Este arquivo ja foi concedido a algum comprador? */
    boolean existsByDigitalAsset_Id(UUID digitalAssetId);

    long countByEntitlement_Id(UUID entitlementId);

    /**
     * Arquivos que uma compra especifica da direito.
     *
     * <p><b>Nao filtra {@code digitalAsset.deletedAt} de proposito.</b> Um arquivo removido
     * do catalogo pelo admin precisa continuar baixando para quem ja pagou por ele —
     * inverter isso quebraria, em silencio, o download de compradores antigos. Somente as
     * visoes de catalogo e de administracao escondem arquivos excluidos.
     */
    @Query("""
    select ea
    from EntitlementAsset ea
    join fetch ea.digitalAsset da
    join fetch da.productPlanType ppt
    join fetch ppt.planType pt
    join ea.entitlement e
    join e.user u
    where u.email = :email
      and e.product.id = :productId
      and upper(e.planType.code) = upper(:collectionCode)
      and e.revokedAt is null
      and e.order.status = com.plantahub.api.domain.orders.enums.OrderStatus.PAID
    order by pt.sortOrder asc, da.sortOrder asc, da.filename asc
  """)
    List<EntitlementAsset> findPinnedForDownload(
            @Param("email") String email,
            @Param("productId") String productId,
            @Param("collectionCode") String collectionCode
    );

    /**
     * Todos os arquivos que o usuario tem direito, para montar a biblioteca numa consulta.
     *
     * <p>Antes disso a biblioteca fazia uma listagem no S3 por produto, a cada requisicao.
     */
    @Query("""
    select ea
    from EntitlementAsset ea
    join fetch ea.digitalAsset da
    join fetch da.productPlanType ppt
    join fetch ppt.planType pt
    join fetch ea.entitlement e
    join e.user u
    where u.email = :email
      and e.revokedAt is null
      and e.order.status = com.plantahub.api.domain.orders.enums.OrderStatus.PAID
    order by pt.sortOrder asc, da.sortOrder asc, da.filename asc
  """)
    List<EntitlementAsset> findPinnedForLibrary(@Param("email") String email);

    /** Ids dos assets ja pinados nesta concessao, para tornar a pinagem idempotente. */
    @Query("""
    select ea.digitalAsset.id
    from EntitlementAsset ea
    where ea.entitlement.id = :entitlementId
  """)
    Set<UUID> findPinnedAssetIds(@Param("entitlementId") UUID entitlementId);
}
