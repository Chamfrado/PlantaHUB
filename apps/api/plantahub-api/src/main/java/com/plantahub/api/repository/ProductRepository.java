package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByStatusOrderByCategoryAscNameAsc(ProductStatus status);

    List<Product> findByCategoryAndStatusOrderByNameAsc(String category, ProductStatus status);

    Optional<Product> findByCategoryAndSlugAndStatus(String category, String slug, ProductStatus status);

    /**
     * Sem filtro de status: usado pelo admin e pela pre-visualizacao, que precisam
     * enxergar rascunhos e arquivados.
     */
    Optional<Product> findByCategoryAndSlug(String category, String slug);

    long countByCategory(String category);

    /**
     * Busca do painel. Parametros nulos nao filtram.
     *
     * <p>Os {@code cast(... as string)} nao sao decoracao: com um parametro nulo sem tipo,
     * o Postgres o infere como {@code bytea} e a consulta estoura com
     * {@code function lower(bytea) does not exist}.
     */
    @Query("""
    select p
    from Product p
    where (:status is null or p.status = :status)
      and (cast(:category as string) is null or p.category = :category)
      and (cast(:query as string) is null
           or lower(p.name) like lower(concat('%', cast(:query as string), '%'))
           or lower(p.id) like lower(concat('%', cast(:query as string), '%'))
           or lower(p.slug) like lower(concat('%', cast(:query as string), '%')))
    order by p.category asc, p.name asc
  """)
    List<Product> search(@Param("status") ProductStatus status,
                         @Param("category") String category,
                         @Param("query") String query);
}
