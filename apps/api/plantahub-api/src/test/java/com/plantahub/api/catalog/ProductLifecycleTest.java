package com.plantahub.api.catalog;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProductLifecycleTest extends AbstractApiTest {

    @Autowired private ProductCatalogService catalogService;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("so produtos PUBLISHED aparecem no catalogo publico")
    void onlyPublishedIsListed() {
        var published = fixtures.product(TestDataFactory.unique("pub"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var draft = fixtures.product(TestDataFactory.unique("dra"), "casas", TestDataFactory.unique("s"), ProductStatus.DRAFT);
        var archived = fixtures.product(TestDataFactory.unique("arc"), "casas", TestDataFactory.unique("s"), ProductStatus.ARCHIVED);

        List<String> ids = catalogService.listProducts("casas", null).stream()
                .map(p -> p.id())
                .toList();

        assertThat(ids).contains(published.getId());
        assertThat(ids).doesNotContain(draft.getId(), archived.getId());
    }

    @Test
    @DisplayName("um rascunho nao resolve pela rota publica de detalhe")
    void draftIsNotReachable() {
        var draft = fixtures.product(TestDataFactory.unique("dra"), "casas", TestDataFactory.unique("s"), ProductStatus.DRAFT);

        // O tipo importa tanto quanto a mensagem: e ele que vira 404, e so em 404 a pagina
        // publica mostra "Produto nao encontrado" em vez da tela de falha com botao de
        // repetir. Com 400, uma URL de produto despublicado convidava a retentar para sempre.
        assertThatThrownBy(() -> catalogService.getProduct(draft.getCategory(), draft.getSlug()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("product_not_found");
    }

    @Test
    @DisplayName("um produto arquivado sai de venda")
    void archivedIsNotReachable() {
        var archived = fixtures.product(TestDataFactory.unique("arc"), "casas", TestDataFactory.unique("s"), ProductStatus.ARCHIVED);

        assertThatThrownBy(() -> catalogService.getProduct(archived.getCategory(), archived.getSlug()))
                .hasMessageContaining("product_not_found");
    }
}
