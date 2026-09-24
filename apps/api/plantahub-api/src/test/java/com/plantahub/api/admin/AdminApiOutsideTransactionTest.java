package com.plantahub.api.admin;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre a API admin <b>sem</b> {@code @Transactional} no teste.
 *
 * <p>Isto importa mais do que parece. Todos os outros testes de integração são
 * transacionais, o que mantém a sessão do Hibernate aberta durante a asserção e esconde
 * uma classe inteira de defeitos: associações preguiçosas lidas depois que a transação
 * fecha. Produção roda com {@code open-in-view: false}, então lá isso vira erro 500.
 *
 * <p>Foi exatamente assim que a tela de ofertas quebrou: o serviço devolvia entidades e o
 * controller lia a coleção já fora da transação. A correção foi montar o DTO dentro do
 * serviço — e este teste existe para que a regressão apareça antes de chegar ao ar.
 */
@AutoConfigureMockMvc
class AdminApiOutsideTransactionTest extends AbstractApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TestDataFactory fixtures;

    private static final org.springframework.test.web.servlet.request.RequestPostProcessor ADMIN =
            user("admin@plantahub.test").roles("ADMIN");

    @Test
    @DisplayName("listar ofertas serializa a coleção sem sessão aberta")
    void offersSerializeOutsideTransaction() throws Exception {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);

        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        mockMvc.perform(get("/v1/admin/products/{id}/offers", product.getId()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectionCode").value(collection.getCode()))
                .andExpect(jsonPath("$[0].collectionName").value(collection.getName()))
                .andExpect(jsonPath("$[0].priceCents").value(150000));
    }

    @Test
    @DisplayName("listar arquivos serializa produto e coleção sem sessão aberta")
    void assetsSerializeOutsideTransaction() throws Exception {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);

        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 1000);
        fixtures.asset(offer, "planta.pdf");

        mockMvc.perform(get("/v1/admin/products/{id}/assets", product.getId()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("planta.pdf"))
                .andExpect(jsonPath("$[0].collectionCode").value(collection.getCode()));
    }

    @Test
    @DisplayName("detalhe do produto serializa conteúdo e mídia sem sessão aberta")
    void productDetailSerializesOutsideTransaction() throws Exception {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);

        mockMvc.perform(get("/v1/admin/products/{id}", product.getId()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.categoryName").value("Casas"));
    }

    @Test
    @DisplayName("a busca de produtos funciona com e sem filtro")
    void productSearchHandlesNullAndPresentFilters() throws Exception {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);

        // Sem filtro nenhum: um parametro nulo sem tipo faz o Postgres inferir bytea e a
        // consulta estourar com "function lower(bytea) does not exist".
        mockMvc.perform(get("/v1/admin/products").with(ADMIN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/admin/products").param("status", "DRAFT").with(ADMIN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/admin/products").param("q", product.getName()).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()));

        mockMvc.perform(get("/v1/admin/products").param("category", "casas").with(ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listar coleções serializa sem sessão aberta")
    void collectionsSerializeOutsideTransaction() throws Exception {
        fixtures.collection(TestDataFactory.uniqueCode("COL"));

        mockMvc.perform(get("/v1/admin/collections").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());
    }

    @Test
    @DisplayName("catálogo público serializa sem sessão aberta")
    void publicCatalogSerializesOutsideTransaction() throws Exception {
        mockMvc.perform(get("/v1/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").exists());
    }
}
