package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.DigitalAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, UUID> {

    /**
     * Arquivos vivos de uma colecao especifica de um produto.
     *
     * <p>Filtra {@code deletedAt} porque no momento da concessao so faz sentido conceder
     * o que esta no catalogo. Depois de pinado, o arquivo continua valendo para aquele
     * comprador mesmo se for removido — por isso o caminho de download <b>nao</b> repete
     * esse filtro.
     */
    @Query("""
    select da
    from DigitalAsset da
    join da.productPlanType ppt
    join ppt.planType pt
    where ppt.product.id = :productId
      and upper(pt.code) = upper(:collectionCode)
      and da.deletedAt is null
    order by da.sortOrder asc, da.filename asc
  """)
    List<DigitalAsset> findActiveByProductAndCollectionCode(
            @Param("productId") String productId,
            @Param("collectionCode") String collectionCode
    );

    /**
     * Arquivos das colecoes que acompanham qualquer oferta deste produto.
     *
     * <p>E a regra que antes era o literal {@code "products/{id}/APOIO/"} repetido em tres
     * servicos. Agora e uma flag consultavel, e a uniao acontece uma unica vez, na compra.
     */
    @Query("""
    select da
    from DigitalAsset da
    join da.productPlanType ppt
    join ppt.planType pt
    where ppt.product.id = :productId
      and pt.bundledWithEveryOffer = true
      and pt.active = true
      and da.deletedAt is null
    order by pt.sortOrder asc, da.sortOrder asc, da.filename asc
  """)
    List<DigitalAsset> findActiveBundledByProduct(@Param("productId") String productId);

    List<DigitalAsset> findByProductPlanType_Id(UUID productPlanTypeId);

    @Query("""
    select count(da)
    from DigitalAsset da
    where da.productPlanType.id = :offerId
      and da.deletedAt is null
  """)
    long countActiveByProductPlanTypeId(@Param("offerId") UUID offerId);

    @Query("""
    select count(da)
    from DigitalAsset da
    where da.productPlanType.planType.id = :collectionId
      and da.deletedAt is null
  """)
    long countActiveByCollectionId(@Param("collectionId") UUID collectionId);

    Optional<DigitalAsset> findByStorageKey(String storageKey);

    /**
     * Um arquivo vendido qualquer, para o diagnostico do bucket conferir que ele
     * <b>nao</b> e legivel sem autenticacao.
     */
    @Query(value = """
        select * from digital_asset
        where deleted_at is null
          and storage_key like 'products/%'
        limit 1
        """, nativeQuery = true)
    Optional<DigitalAsset> findAnyPaidSample();

    /** Extensoes distintas dos arquivos vivos das colecoes compraveis de um produto. */
    @Query("""
    select distinct da.fileExt
    from DigitalAsset da
    join da.productPlanType ppt
    join ppt.planType pt
    where ppt.product.id = :productId
      and da.deletedAt is null
      and da.fileExt is not null
      and pt.purchasable = true
      and pt.active = true
  """)
    List<String> findPurchasableFileExtensions(@Param("productId") String productId);

    @Query("""
    select da
    from DigitalAsset da
    join fetch da.productPlanType ppt
    join fetch ppt.product p
    join fetch ppt.planType pt
    where p.id = :productId
  """)
    List<DigitalAsset> findByProductId(@Param("productId") String productId);

    @Query("""
    select da
    from DigitalAsset da
    join fetch da.productPlanType ppt
    join fetch ppt.product p
    join fetch ppt.planType pt
  """)
    List<DigitalAsset> findAllWithRelations();
}
