package com.plantahub.api.support;

import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.shared.storage.StoredObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bucket em memoria. Substitui o S3 na grande maioria dos testes.
 *
 * <p>O que realmente precisa de teste — geracao de chave, mapeamento pasta->colecao,
 * classificacao da reconciliacao, pinagem de entitlement, autorizacao — nao tem nada a
 * ver com a semantica do S3. Rodar isso contra um duplo em memoria custa milissegundos e
 * nao exige Docker. O que um duplo nao consegue provar (um PUT presignado real aceita o
 * corpo? multipart completa?) tambem nao seria provado por nenhum fake, e por isso vive
 * num unico teste opt-in contra LocalStack (perfil Maven {@code integration}).
 */
public class InMemoryObjectStorage implements ObjectStoragePort {

    private final ConcurrentSkipListMap<String, Entry> objects = new ConcurrentSkipListMap<>();

    /** Quando ligado, {@link #list(String)} lanca. Usado para provar que o S3 saiu do caminho de leitura. */
    private final AtomicBoolean failOnList = new AtomicBoolean(false);

    private final java.util.Map<String, Multipart> multipartUploads = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<String> abortedUploads = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record Entry(byte[] content, String contentType, Instant lastModified) {}

    // ---------- helpers de teste ----------

    public void seed(String key, String content) {
        seed(key, content.getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/octet-stream");
    }

    public void seed(String key, byte[] content, String contentType) {
        objects.put(key, new Entry(content, contentType, Instant.now()));
    }

    public void clear() {
        objects.clear();
        multipartUploads.clear();
        abortedUploads.clear();
        failOnList.set(false);
    }

    public void failOnList(boolean fail) {
        failOnList.set(fail);
    }

    public List<String> keys() {
        return new ArrayList<>(objects.keySet());
    }

    public boolean contains(String key) {
        return objects.containsKey(key);
    }

    // ---------- ObjectStoragePort ----------

    @Override
    public List<StoredObject> list(String prefix) {
        if (failOnList.get()) {
            throw new IllegalStateException("list() nao deveria ser chamado neste teste");
        }

        List<StoredObject> result = new ArrayList<>();

        for (Map.Entry<String, Entry> e : objects.tailMap(prefix).entrySet()) {
            if (!e.getKey().startsWith(prefix)) {
                break; // mapa ordenado: passou do prefixo, acabou
            }
            if (e.getKey().endsWith("/")) {
                continue; // marcador de diretorio, igual ao adaptador real
            }
            result.add(toStoredObject(e.getKey(), e.getValue(), false));
        }

        return result;
    }

    @Override
    public Optional<StoredObject> head(String key) {
        Entry entry = objects.get(key);
        return entry == null
                ? Optional.empty()
                : Optional.of(toStoredObject(key, entry, true));
    }

    @Override
    public InputStream open(String key) {
        Entry entry = objects.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("no_such_key: " + key);
        }
        return new ByteArrayInputStream(entry.content());
    }

    @Override
    public void put(String key, Path file, String contentType) {
        try {
            objects.put(key, new Entry(Files.readAllBytes(file), contentType, Instant.now()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String presignGet(String key, Duration ttl, String downloadFilename) {
        return "https://test-bucket.local/" + key + "?expires=" + ttl.toSeconds();
    }

    // ---------- escrita pelo navegador ----------

    @Override
    public String presignPut(String key, Duration ttl, String contentType) {
        return "https://test-bucket.local/" + key + "?upload=put&expires=" + ttl.toSeconds();
    }

    @Override
    public String initiateMultipart(String key, String contentType) {
        String uploadId = "mpu-" + UUID.randomUUID();
        multipartUploads.put(uploadId, new Multipart(key, contentType));
        return uploadId;
    }

    @Override
    public String presignUploadPart(String key, String uploadId, int partNumber, Duration ttl) {
        return "https://test-bucket.local/" + key + "?uploadId=" + uploadId + "&part=" + partNumber;
    }

    @Override
    public void completeMultipart(String key, String uploadId, List<PartETag> parts) {
        Multipart upload = multipartUploads.remove(uploadId);

        if (upload == null) {
            throw new IllegalArgumentException("no_such_upload: " + uploadId);
        }

        // Grava um objeto representando o arquivo montado. O conteudo nao importa aqui;
        // o que os testes verificam e que ele passa a existir e com que metadados.
        seed(key, ("multipart:" + parts.size() + ":parts")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8), upload.contentType());
    }

    @Override
    public void abortMultipart(String key, String uploadId) {
        if (multipartUploads.remove(uploadId) == null) {
            throw new IllegalArgumentException("no_such_upload: " + uploadId);
        }
        abortedUploads.add(uploadId);
    }

    /** Multiparts abortados, para os testes do varredor. */
    public boolean wasAborted(String uploadId) {
        return abortedUploads.contains(uploadId);
    }

    public boolean hasOpenMultipart(String uploadId) {
        return multipartUploads.containsKey(uploadId);
    }

    private record Multipart(String key, String contentType) {}

    private StoredObject toStoredObject(String key, Entry entry, boolean includeContentType) {
        return new StoredObject(
                key,
                (long) entry.content().length,
                entry.lastModified(),
                "etag-" + Integer.toHexString(java.util.Arrays.hashCode(entry.content())),
                // igual ao S3 real: content-type so vem no head, nunca na listagem
                includeContentType ? entry.contentType() : null
        );
    }
}
