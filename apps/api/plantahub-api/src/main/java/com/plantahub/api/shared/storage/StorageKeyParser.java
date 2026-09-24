package com.plantahub.api.shared.storage;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * Traduz uma chave do bucket na estrutura que o catalogo entende.
 *
 * <p>Esta e a <b>unica</b> implementacao da regra "primeiro segmento depois do produto e o
 * nome da colecao". A reconciliacao do bucket legado e o upload de uma pasta pelo painel
 * usam as duas a mesma classe, o que faz com que "jogar uma pasta ARCH/ inteira" e
 * "reconciliar o que ja esta la" concordem por construcao, e nao por coincidencia.
 *
 * <p><b>Atencao:</b> isto interpreta chaves que ja existem. Nao e permitido usar esta
 * classe para <i>montar</i> uma chave na hora de ler um arquivo — a chave de leitura vem
 * sempre de {@code digital_asset.storage_key}.
 */
public final class StorageKeyParser {

    public static final String PRODUCTS_PREFIX = "products/";

    /** Extensoes tratadas como midia de vitrine, nunca como arquivo entregue ao cliente. */
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "avif", "gif", "svg");

    public enum Shape {
        /** {@code products/{produto}/{COLECAO}/[subpasta/...]arquivo} */
        COLLECTION_FILE,
        /** {@code products/{produto}/arquivo} — sem colecao. Normalmente a capa. */
        DIRECTLY_UNDER_PRODUCT,
        /** Nao segue nenhum formato reconhecido. */
        UNPARSEABLE
    }

    public record ParsedKey(
            Shape shape,
            String productId,
            String folder,
            String relativePath,
            String filename
    ) {
        public boolean isImage() {
            return filename != null && IMAGE_EXTENSIONS.contains(extension());
        }

        public String extension() {
            if (filename == null) return "";
            int dot = filename.lastIndexOf('.');
            if (dot < 0 || dot == filename.length() - 1) return "";
            return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
    }

    private StorageKeyParser() {
    }

    public static ParsedKey parse(String key) {
        if (key == null || key.isBlank() || key.endsWith("/") || !key.startsWith(PRODUCTS_PREFIX)) {
            return new ParsedKey(Shape.UNPARSEABLE, null, null, null, null);
        }

        String[] segments = key.split("/");

        // segments[0] = "products"
        if (segments.length < 3 || segments[1].isBlank()) {
            return new ParsedKey(Shape.UNPARSEABLE, null, null, null, null);
        }

        String productId = segments[1];
        String filename = segments[segments.length - 1];

        if (segments.length == 3) {
            // products/{produto}/arquivo
            return new ParsedKey(Shape.DIRECTLY_UNDER_PRODUCT, productId, null, null, filename);
        }

        String folder = segments[2];

        // Tudo entre a pasta da colecao e o arquivo. E o que permite reconstruir a arvore
        // de um upload de pasta, e o que preserva o "v1/" das chaves legadas.
        String relativePath = segments.length > 4
                ? String.join("/", Arrays.copyOfRange(segments, 3, segments.length - 1))
                : null;

        return new ParsedKey(Shape.COLLECTION_FILE, productId, folder, relativePath, filename);
    }

    /** Normaliza o nome de uma pasta para comparar com {@code plan_type.code}. */
    public static String normalizeFolder(String folder) {
        return folder == null ? null : folder.trim().toUpperCase(Locale.ROOT);
    }
}
