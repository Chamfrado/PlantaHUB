package com.plantahub.api.shared.storage;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Gera as chaves dos arquivos novos.
 *
 * <p>Formato: {@code products/{produto}/{COLECAO}/{idDoAsset}/[subpasta/]arquivo}
 *
 * <p>O segmento com o id do asset resolve colisão por construção: dois uploads do mesmo
 * "Planta Arquitetônica.pdf" na mesma coleção nunca disputam a mesma chave, sem contador e
 * sem laço de repetição. Produto e coleção continuam legíveis porque o dono do bucket
 * administra os arquivos pelo console da AWS e vai continuar fazendo isso.
 *
 * <p><b>Não existe segmento de versão no caminho.</b> Versionar é responsabilidade do
 * banco ({@code digital_asset.version}); o {@code /v1/} das chaves antigas é justamente o
 * resíduo de quando o caminho era reconstruído por convenção.
 *
 * <p>A chave gerada aqui é gravada uma vez e nunca mais recalculada.
 */
@Component
public class StorageKeyFactory {

    /** Limite da coluna {@code digital_asset.storage_key}. */
    public static final int MAX_KEY_LENGTH = 500;

    /**
     * Raiz de tudo que e legivel sem autenticacao.
     *
     * <p>Fora de {@link StorageKeyParser#PRODUCTS_PREFIX} de proposito: a reconciliacao
     * varre {@code products/} procurando arquivos vendaveis, e imagem de vitrine nao e um
     * deles.
     */
    public static final String PUBLIC_PREFIX = "public/products/";

    private static final int MAX_BASENAME_LENGTH = 120;
    private static final String FALLBACK_BASENAME = "arquivo";

    /**
     * @param keySegment segmento aleatorio que garante unicidade. Deliberadamente
     *                   independente do id da linha: pre-atribuir o id de uma entidade com
     *                   {@code @GeneratedValue} faz o JPA tratar a insercao como update.
     */
    public record GeneratedKey(UUID keySegment, String key, String normalizedFilename) {}

    /**
     * @param relativePath caminho dentro da coleção, quando o arquivo veio de um upload de
     *                     pasta. Pode ser {@code null}.
     */
    public GeneratedKey create(String productId, String collectionCode,
                               String relativePath, String originalFilename) {
        UUID keySegment = UUID.randomUUID();
        String filename = normalizeFilename(originalFilename);

        StringBuilder key = new StringBuilder(StorageKeyParser.PRODUCTS_PREFIX)
                .append(productId)
                .append('/')
                .append(collectionCode.toUpperCase(Locale.ROOT))
                .append('/')
                .append(keySegment);

        String subpath = normalizeRelativePath(relativePath);
        if (subpath != null) {
            key.append('/').append(subpath);
        }

        key.append('/').append(filename);

        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("storage_key_too_long");
        }

        return new GeneratedKey(keySegment, key.toString(), filename);
    }

    /**
     * Chave de uma imagem de vitrine: capa ou item da galeria.
     *
     * <p>Formato: {@code public/products/{produto}/{segmento}/arquivo}
     *
     * <p><b>O prefixo separado e o ponto inteiro deste metodo.</b> Capa e galeria precisam
     * ser legiveis por qualquer visitante, sem autenticacao; os arquivos vendidos, que
     * vivem sob {@code products/}, nao podem ser legiveis por ninguem sem direito. Manter
     * os dois sob a mesma raiz obrigaria a politica do bucket a liberar um curinga no meio
     * do caminho ({@code products/*}{@code /MEDIA/*}) — o tipo de regra que alguem alarga
     * para {@code products/*} no dia em que uma imagem aparece quebrada, publicando o
     * acervo inteiro junto. Com {@code public/}, a politica e uma linha obvia e o erro
     * deixa de estar disponivel.
     */
    public GeneratedKey createMediaKey(String productId, String originalFilename) {
        UUID keySegment = UUID.randomUUID();
        String filename = normalizeFilename(originalFilename);

        String key = PUBLIC_PREFIX + productId + '/' + keySegment + '/' + filename;

        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("storage_key_too_long");
        }

        return new GeneratedKey(keySegment, key, filename);
    }

    /**
     * Deixa o nome seguro para uma chave S3.
     *
     * <p>Os nomes reais são em português e cheios de acento, espaço e parêntese — coisas
     * que sobrevivem numa chave S3 mas exigem escape em URL e viram fonte constante de
     * confusão. O nome de exibição original continua em {@code digital_asset.filename}.
     */
    public String normalizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return FALLBACK_BASENAME;
        }

        // Descartar tudo antes da ultima barra JA e a defesa contra travessia: o navegador
        // manda o caminho completo em alguns casos, e depois deste corte nao sobra caminho
        // nenhum para escapar.
        String cleaned = original.trim().replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1);

        rejectControlCharacters(cleaned);

        int dot = cleaned.lastIndexOf('.');
        String base = dot > 0 ? cleaned.substring(0, dot) : cleaned;
        String extension = dot > 0 ? cleaned.substring(dot + 1) : "";

        String slug = slugify(base);

        if (slug.isBlank()) {
            slug = FALLBACK_BASENAME;
        }
        if (slug.length() > MAX_BASENAME_LENGTH) {
            slug = slug.substring(0, MAX_BASENAME_LENGTH);
        }

        String normalizedExtension = extension
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");

        if (normalizedExtension.isBlank() || normalizedExtension.length() > 10) {
            return slug;
        }

        return slug + "." + normalizedExtension;
    }

    /** Aplica a mesma normalização a cada segmento do subcaminho. */
    public String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        rejectControlCharacters(relativePath);
        rejectTraversal(relativePath);

        String joined = Arrays.stream(relativePath.replace('\\', '/').split("/"))
                .map(this::slugify)
                .filter(segment -> !segment.isBlank())
                .reduce((a, b) -> a + "/" + b)
                .orElse("");

        return joined.isBlank() ? null : joined;
    }

    private String slugify(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+|[-.]+$", "");
    }

    /**
     * Recusa travessia de diretorio no subcaminho.
     *
     * <p>Compara <b>segmento a segmento</b>, e nao por substring: um nome legitimo como
     * {@code relatorio..final} contem "..", mas nao sobe nenhum nivel.
     */
    private void rejectTraversal(String value) {
        boolean escapes = Arrays.stream(value.replace('\\', '/').split("/"))
                .anyMatch(segment -> segment.equals("..") || segment.equals("."));

        if (escapes || value.startsWith("/")) {
            throw new IllegalArgumentException("invalid_relative_path");
        }
    }

    private void rejectControlCharacters(String value) {
        if (value.chars().anyMatch(c -> c < 0x20)) {
            throw new IllegalArgumentException("invalid_filename");
        }
    }
}
