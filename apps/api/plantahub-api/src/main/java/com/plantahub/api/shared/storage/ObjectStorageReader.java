package com.plantahub.api.shared.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Acesso somente-leitura ao armazenamento de objetos.
 *
 * <p>Esta interface existe para ser injetada em código que <b>nunca</b> pode escrever nem
 * apagar — em especial o job de reconciliação, cuja garantia de "nunca destrói dados" é
 * estrutural (não há método de escrita para chamar), e não uma convenção que uma revisão
 * futura possa violar sem perceber.
 *
 * <p>Não adicione métodos de escrita aqui. Eles pertencem a {@link ObjectStoragePort}.
 */
public interface ObjectStorageReader {

    /**
     * Lista recursivamente todos os objetos sob o prefixo, paginando até o fim.
     * Marcadores de diretório (chaves terminadas em {@code /}) são omitidos.
     */
    List<StoredObject> list(String prefix);

    /** Metadados do objeto, ou vazio se ele não existe. */
    Optional<StoredObject> head(String key);

    /** Abre o conteúdo do objeto. O chamador é responsável por fechar o stream. */
    InputStream open(String key);
}
