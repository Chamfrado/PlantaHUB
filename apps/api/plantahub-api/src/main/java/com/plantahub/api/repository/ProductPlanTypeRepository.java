package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.ProductPlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPlanTypeRepository extends JpaRepository<ProductPlanType, UUID> {

    @Query("""
    select ppt
    from ProductPlanType ppt
    join fetch ppt.planType pt
    where ppt.product.id = :productId
      and ppt.available = true
    order by ppt.sortOrder asc
  """)
    List<ProductPlanType> findAvailableByProductIdWithPlanType(String productId);

    /**
     * Ofertas que o cliente pode de fato comprar.
     *
     * <p>Filtra por {@code planType.purchasable} e nao apenas por {@code available}: uma
     * colecao de anexos (que acompanha toda oferta) tem linha em product_plan_type para
     * poder ancorar arquivos, mas nunca pode aparecer no seletor nem no checkout.
     */
    @Query("""
    select ppt
    from ProductPlanType ppt
    join fetch ppt.planType pt
    where ppt.product.id = :productId
      and ppt.available = true
      and pt.purchasable = true
      and pt.active = true
    order by ppt.sortOrder asc
  """)
    List<ProductPlanType> findPurchasableByProductIdWithPlanType(String productId);

    /** Colecoes cujos arquivos acompanham qualquer oferta comprada deste produto. */
    @Query("""
    select ppt
    from ProductPlanType ppt
    join fetch ppt.planType pt
    where ppt.product.id = :productId
      and pt.bundledWithEveryOffer = true
      and pt.active = true
    order by ppt.sortOrder asc
  """)
    List<ProductPlanType> findBundledByProductIdWithPlanType(String productId);

    /**
     * Menor preco a venda de cada produto, em uma consulta so.
     *
     * <p>E o "a partir de" da vitrine. Deriva das ofertas em vez de ler
     * {@code product.base_price_cents}, que era uma coluna desnormalizada que ninguem
     * atualizava: o painel define preco por oferta, e a coluna ficava em zero para sempre
     * — a vitrine anunciava R$ 0,00 para produto com cinco ofertas pagas.
     *
     * <p>Em lote por id porque a alternativa e uma consulta por produto dentro do laco que
     * monta a lista, que e exatamente o N+1 que uma vitrine nao pode ter.
     */
    @Query("""
    select ppt.product.id, min(ppt.priceCents)
    from ProductPlanType ppt
    join ppt.planType pt
    where ppt.product.id in :productIds
      and ppt.available = true
      and pt.purchasable = true
      and pt.active = true
      and ppt.priceCents > 0
    group by ppt.product.id
  """)
    List<Object[]> findMinPurchasablePriceByProductIds(@Param("productIds") Collection<String> productIds);

    Optional<ProductPlanType> findByProduct_IdAndPlanType_Code(String productId, String planTypeCode);

    List<ProductPlanType> findAllByProduct_Id(String productId);

    List<ProductPlanType> findAllByProduct_IdIn(Collection<String> productIds);

    List<ProductPlanType> findAllByPlanType_Id(UUID planTypeId);

    /**
     * Ofertas de um produto com a colecao ja carregada.
     *
     * <p>{@code findAllByProduct_Id} devolve a colecao como proxy preguicoso. Producao roda
     * com {@code open-in-view: false}, entao qualquer leitura dela depois que a transacao
     * fecha estoura LazyInitializationException.
     */
    @Query("""
    select ppt
    from ProductPlanType ppt
    join fetch ppt.planType pt
    where ppt.product.id = :productId
    order by ppt.sortOrder asc, pt.code asc
  """)
    List<ProductPlanType> findAllByProductIdWithPlanType(@Param("productId") String productId);
}
