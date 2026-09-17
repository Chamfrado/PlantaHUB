package com.plantahub.api.domain.catalog.enums;

/**
 * Ciclo de vida de um produto no catalogo.
 *
 * <p>Substitui o boolean {@code active}, que nao conseguia distinguir "o admin ainda
 * esta montando isto" de "isto ja foi vendido e saiu de linha" — duas situacoes com
 * regras opostas sobre historico e sobre o que pode ser apagado.
 */
public enum ProductStatus {

    /** Em construcao. Nao aparece no catalogo publico e nao pode ser comprado. */
    DRAFT,

    /** Visivel e comprável. */
    PUBLISHED,

    /**
     * Fora de venda, mas preservado. Continua resolvendo para pedidos, entitlements e
     * downloads antigos — arquivar nunca pode quebrar uma compra ja paga.
     */
    ARCHIVED
}
