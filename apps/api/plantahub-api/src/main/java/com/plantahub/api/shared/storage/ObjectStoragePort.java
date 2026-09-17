package com.plantahub.api.shared.storage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Porta de acesso ao armazenamento de objetos (hoje, AWS S3).
 *
 * <p>É a única fronteira da aplicação com o SDK da AWS: apenas
 * {@link S3ObjectStorageAdapter} conhece {@code software.amazon.awssdk}. Isso permite
 * substituir o armazenamento por um duplo em memória nos testes, sem Docker e sem rede.
 *
 * <p><b>Regra</b>: chaves são opacas. Nenhum implementador nem chamador desta porta pode
 * montar uma chave por convenção a partir de id de produto, código de coleção ou nome de
 * pasta. A chave vem de {@code digital_asset.storage_key} ou do processo de upload.
 */
public interface ObjectStoragePort extends ObjectStorageReader {

    /** Envia um arquivo local para a chave informada. */
    void put(String key, Path file, String contentType);

    /**
     * Gera uma URL temporária de download.
     *
     * @param downloadFilename nome sugerido ao navegador via {@code Content-Disposition};
     *                         se {@code null}, o nome é derivado do último segmento da chave.
     */
    String presignGet(String key, Duration ttl, String downloadFilename);

    /** Conveniência: {@code presignGet} deixando o adaptador derivar o nome do arquivo. */
    default String presignGet(String key, Duration ttl) {
        return presignGet(key, ttl, null);
    }

    /** {@code true} se o objeto existe. Atalho de leitura sobre {@link #head(String)}. */
    default boolean exists(String key) {
        return head(key).isPresent();
    }

    // ------------------------------------------------------------------
    // Escrita pelo navegador
    //
    // A aplicação nunca intermedia os bytes de um upload: ela assina uma URL e o navegador
    // envia direto ao bucket. Passar arquivos de centenas de megabytes por dentro da API
    // consumiria memória e tempo de request sem nenhum ganho.
    // ------------------------------------------------------------------

    /**
     * URL assinada para um PUT único.
     *
     * <p>O {@code contentType} entra na assinatura: o navegador precisa enviar exatamente
     * este cabeçalho, ou a S3 recusa com {@code SignatureDoesNotMatch}.
     */
    String presignPut(String key, Duration ttl, String contentType);

    /** Inicia um upload multipart e devolve o identificador dele na S3. */
    String initiateMultipart(String key, String contentType);

    /** URL assinada para enviar uma parte específica. */
    String presignUploadPart(String key, String uploadId, int partNumber, Duration ttl);

    /**
     * Fecha o multipart juntando as partes.
     *
     * <p>Exige o ETag de cada parte, que o navegador só consegue ler se o CORS do bucket
     * expuser o cabeçalho {@code ETag}.
     */
    void completeMultipart(String key, String uploadId, List<PartETag> parts);

    /** Cancela um multipart. Partes órfãs continuam sendo cobradas até serem abortadas. */
    void abortMultipart(String key, String uploadId);

    record PartETag(int partNumber, String eTag) {}
}
