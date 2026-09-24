package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.uploads.PendingUpload;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.PendingUploadRepository;
import com.plantahub.api.repository.PlanTypeRepository;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.shared.storage.PublicMediaUrls;
import com.plantahub.api.shared.storage.StorageKeyFactory;
import com.plantahub.api.shared.storage.StoredObject;
import com.plantahub.api.web.dto.admin.UploadDTOs.CompleteMultipartRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.ConfirmUploadRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.ConfirmUploadResponse;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignResponse;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignedPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Upload direto do navegador para o bucket, em dois passos.
 *
 * <p><b>Passo 1 — presign.</b> O servidor escolhe a chave, grava um {@code pending_upload}
 * e devolve URLs assinadas. A chave nunca vem do cliente.
 *
 * <p><b>Passo 2 — confirm.</b> O servidor consulta o próprio bucket ({@code HeadObject})
 * e usa o tamanho e o tipo <b>reais</b> para criar a linha. O que o cliente declarou serve
 * apenas para escolher entre PUT único e multipart; confiar nele permitiria registrar um
 * arquivo com metadados inventados, ou registrar um arquivo que nem existe.
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final AppUserRepository userRepo;
    private final ProductRepository productRepo;
    private final PlanTypeRepository collectionRepo;
    private final ProductPlanTypeRepository offerRepo;
    private final DigitalAssetRepository assetRepo;
    private final ProductMediaRepository mediaRepo;
    private final PendingUploadRepository pendingRepo;
    private final ObjectStoragePort storage;
    private final StorageKeyFactory keyFactory;

    private final long maxSizeBytes;
    private final long multipartThresholdBytes;
    private final long partSizeBytes;
    private final Duration presignTtl;
    private final Duration multipartPresignTtl;
    private final Set<String> allowedExtensions;
    private final Set<String> mediaExtensions;
    private final long mediaMaxSizeBytes;
    private final PublicMediaUrls publicUrls;

    public UploadService(
            AppUserRepository userRepo,
            ProductRepository productRepo,
            PlanTypeRepository collectionRepo,
            ProductPlanTypeRepository offerRepo,
            DigitalAssetRepository assetRepo,
            ProductMediaRepository mediaRepo,
            PendingUploadRepository pendingRepo,
            ObjectStoragePort storage,
            StorageKeyFactory keyFactory,
            @Value("${app.uploads.max-size-bytes:2147483648}") long maxSizeBytes,
            @Value("${app.uploads.multipart-threshold-bytes:104857600}") long multipartThresholdBytes,
            @Value("${app.uploads.part-size-bytes:16777216}") long partSizeBytes,
            @Value("${app.uploads.presign-ttl-seconds:900}") long presignTtlSeconds,
            @Value("${app.uploads.multipart-presign-ttl-seconds:3600}") long multipartTtlSeconds,
            @Value("${app.uploads.allowed-extensions:}") String allowedExtensions,
            @Value("${app.uploads.media-allowed-extensions:}") String mediaExtensions,
            @Value("${app.uploads.media-max-size-bytes:10485760}") long mediaMaxSizeBytes,
            PublicMediaUrls publicUrls
    ) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.collectionRepo = collectionRepo;
        this.offerRepo = offerRepo;
        this.assetRepo = assetRepo;
        this.mediaRepo = mediaRepo;
        this.pendingRepo = pendingRepo;
        this.storage = storage;
        this.keyFactory = keyFactory;
        this.maxSizeBytes = maxSizeBytes;
        this.multipartThresholdBytes = multipartThresholdBytes;
        this.partSizeBytes = partSizeBytes;
        this.presignTtl = Duration.ofSeconds(presignTtlSeconds);
        this.multipartPresignTtl = Duration.ofSeconds(multipartTtlSeconds);
        this.allowedExtensions = parseSet(allowedExtensions);
        this.mediaExtensions = parseSet(mediaExtensions);
        this.mediaMaxSizeBytes = mediaMaxSizeBytes;
        this.publicUrls = publicUrls;
    }

    // ------------------------------------------------------------------
    // Passo 1 — autorizar
    // ------------------------------------------------------------------

    @Transactional
    public PresignResponse presign(String email, PresignRequest request) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new NotFoundException("user_not_found"));

        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("product_not_found"));

        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new ConflictException("product_archived");
        }

        boolean isMedia = request.targetKind().isPublicImage();

        validateSize(request.sizeBytes(), isMedia);

        String filename = keyFactory.normalizeFilename(request.filename());
        validateExtension(filename, isMedia);

        ProductPlanType offer = null;
        String collectionCode;

        if (isMedia) {
            // Mídia não pertence a coleção nenhuma: ela nunca é entregue como arquivo.
            collectionCode = "MEDIA";
        } else {
            collectionCode = request.collectionCode().trim().toUpperCase(Locale.ROOT);
            offer = resolveOffer(product, collectionCode);
        }

        var generated = isMedia
                ? keyFactory.createMediaKey(product.getId(), request.filename())
                : keyFactory.create(
                        product.getId(), collectionCode, request.relativePath(), request.filename());

        String contentType = request.contentType() != null && !request.contentType().isBlank()
                ? request.contentType()
                : "application/octet-stream";

        boolean multipart = !isMedia
                && request.sizeBytes() != null
                && request.sizeBytes() > multipartThresholdBytes;

        PendingUpload pending = PendingUpload.builder()
                .createdBy(user)
                .targetKind(request.targetKind())
                .product(product)
                .productPlanType(offer)
                .keySegment(generated.keySegment())
                .storageKey(generated.key())
                .originalFilename(request.filename())
                .relativePath(keyFactory.normalizeRelativePath(request.relativePath()))
                .declaredContentType(contentType)
                .declaredSizeBytes(request.sizeBytes())
                .expiresAt(Instant.now().plus(multipart ? multipartPresignTtl : presignTtl))
                .build();

        if (!multipart) {
            pendingRepo.save(pending);

            return PresignResponse.singlePut(
                    pending.getId(),
                    generated.key(),
                    storage.presignPut(generated.key(), presignTtl, contentType),
                    contentType,
                    presignTtl.toSeconds()
            );
        }

        String uploadId = storage.initiateMultipart(generated.key(), contentType);
        pending.setMultipartUploadId(uploadId);
        pendingRepo.save(pending);

        int partCount = (int) Math.ceil((double) request.sizeBytes() / partSizeBytes);

        return PresignResponse.multipart(
                pending.getId(),
                generated.key(),
                uploadId,
                partSizeBytes,
                presignParts(generated.key(), uploadId, 1, partCount),
                multipartPresignTtl.toSeconds()
        );
    }

    /**
     * Mais URLs de parte para um upload longo.
     *
     * <p>Um arquivo de 2 GB tem 128 partes; assinar todas de uma vez produziria uma
     * resposta enorme e URLs que podem expirar antes de serem usadas.
     */
    @Transactional(readOnly = true)
    public List<PresignedPart> moreParts(UUID uploadId, int fromPartNumber, int count) {
        PendingUpload pending = requirePending(uploadId);

        if (!pending.isMultipart()) {
            throw new ConflictException("upload_is_not_multipart");
        }

        int safeCount = Math.min(Math.max(count, 1), 100);

        return presignParts(
                pending.getStorageKey(),
                pending.getMultipartUploadId(),
                Math.max(fromPartNumber, 1),
                Math.max(fromPartNumber, 1) + safeCount - 1
        );
    }

    @Transactional
    public void completeMultipart(UUID uploadId, CompleteMultipartRequest request) {
        PendingUpload pending = requirePending(uploadId);

        if (!pending.isMultipart()) {
            throw new ConflictException("upload_is_not_multipart");
        }

        var parts = request.parts().stream()
                .map(part -> new ObjectStoragePort.PartETag(part.partNumber(), part.etag()))
                .toList();

        storage.completeMultipart(pending.getStorageKey(), pending.getMultipartUploadId(), parts);
    }

    // ------------------------------------------------------------------
    // Passo 2 — confirmar
    // ------------------------------------------------------------------

    /**
     * Registra o arquivo, com os metadados lidos do próprio bucket.
     *
     * <p>Idempotente: confirmar de novo devolve o registro já criado, em vez de duplicar.
     */
    @Transactional
    public UUID confirm(UUID uploadId, ConfirmUploadRequest request) {
        PendingUpload pending = pendingRepo.findById(uploadId)
                .orElseThrow(() -> new NotFoundException("upload_not_found"));

        if (pending.getStatus() == PendingUpload.Status.CONFIRMED) {
            return existingRecordId(pending);
        }

        if (pending.getStatus() != PendingUpload.Status.PENDING) {
            throw new ConflictException("upload_not_pending");
        }

        // A fonte da verdade sobre o que foi realmente gravado é o bucket, não o cliente.
        StoredObject stored = storage.head(pending.getStorageKey())
                .orElseThrow(() -> new ConflictException("upload_object_missing"));

        UUID recordId = switch (pending.getTargetKind()) {
            case MEDIA -> createMedia(pending, stored);
            case ASSET -> createAsset(pending, stored, request);
            // Nada a registrar: a URL volta para o painel, que a grava no conteúdo.
            case CONTENT_IMAGE -> pending.getId();
        };

        pending.setStatus(PendingUpload.Status.CONFIRMED);
        pending.setConfirmedAt(Instant.now());
        pendingRepo.save(pending);

        log.info("Upload {} confirmado: {} ({} bytes)",
                uploadId, pending.getStorageKey(), stored.sizeBytes());

        return recordId;
    }

    /** Como {@link #confirm}, devolvendo também a URL pública quando o alvo é uma imagem. */
    @Transactional
    public ConfirmUploadResponse confirmWithUrl(UUID uploadId, ConfirmUploadRequest request) {
        UUID id = confirm(uploadId, request);
        PendingUpload pending = pendingRepo.findById(uploadId).orElseThrow();

        String publicUrl = pending.getTargetKind().isPublicImage()
                ? publicUrls.urlFor(pending.getStorageKey())
                : null;

        return new ConfirmUploadResponse(id, pending.getStorageKey(), publicUrl);
    }

    @Transactional
    public void abort(UUID uploadId) {
        PendingUpload pending = requirePending(uploadId);

        if (pending.isMultipart()) {
            // Sem abortar, as partes já enviadas continuam sendo cobradas indefinidamente.
            storage.abortMultipart(pending.getStorageKey(), pending.getMultipartUploadId());
        }

        pending.setStatus(PendingUpload.Status.ABORTED);
        pendingRepo.save(pending);
    }

    // ------------------------------------------------------------------

    private UUID createAsset(PendingUpload pending, StoredObject stored,
                             ConfirmUploadRequest request) {
        String extension = extensionOf(pending.getStorageKey());

        // Sem id pre-atribuido: a entidade usa @GeneratedValue, e informar o id faria o
        // Spring Data emitir um UPDATE de uma linha que ainda nao existe.
        DigitalAsset asset = assetRepo.save(DigitalAsset.builder()
                .productPlanType(pending.getProductPlanType())
                .version(1)
                // Nome de exibição: o original, com acentos e espaços preservados.
                .filename(pending.getOriginalFilename())
                .storageKey(pending.getStorageKey())
                .sizeBytes(stored.sizeBytes())
                .etag(stored.eTag())
                .checksumSha256(request == null ? null : request.checksumSha256())
                .mediaType(stored.contentType() != null
                        ? stored.contentType()
                        : pending.getDeclaredContentType())
                .fileExt(extension.isBlank() ? null : extension)
                .relativePath(pending.getRelativePath())
                .kind(request != null && request.kind() != null ? request.kind() : "FILE")
                .keyScheme("V2")
                .reconciliationStatus("MATCHED")
                .sortOrder(0)
                .createdBy(pending.getCreatedBy())
                .createdAt(Instant.now())
                .build());

        return asset.getId();
    }

    private UUID createMedia(PendingUpload pending, StoredObject stored) {
        ProductMedia media = mediaRepo.save(ProductMedia.builder()
                .product(pending.getProduct())
                // Nasce na galeria: promover a capa é uma ação explícita, porque só pode
                // haver uma e trocar a capa muda a vitrine.
                .role(ProductMedia.Role.GALLERY)
                .storageKey(pending.getStorageKey())
                // Sem isto a linha nasce sem URL e a imagem chega ao site como caminho
                // relativo, que resolve contra a origem do proprio site e da 404.
                .publicUrl(publicUrls.urlFor(pending.getStorageKey()))
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .sortOrder(0)
                .build());

        return media.getId();
    }

    /**
     * O registro ja criado por uma confirmacao anterior.
     *
     * <p>Encontrado pela chave de armazenamento, que e unica — e o vinculo confiavel entre
     * o upload e a linha resultante.
     */
    private UUID existingRecordId(PendingUpload pending) {
        if (pending.getTargetKind() == PendingUpload.TargetKind.CONTENT_IMAGE) {
            return pending.getId();
        }

        if (pending.getTargetKind() == PendingUpload.TargetKind.MEDIA) {
            return mediaRepo
                    .findByProduct_IdAndDeletedAtIsNullOrderByRoleAscSortOrderAsc(
                            pending.getProduct().getId())
                    .stream()
                    .filter(media -> media.getStorageKey().equals(pending.getStorageKey()))
                    .findFirst()
                    .map(ProductMedia::getId)
                    .orElseThrow(() -> new NotFoundException("confirmed_media_not_found"));
        }

        return assetRepo.findByStorageKey(pending.getStorageKey())
                .map(DigitalAsset::getId)
                .orElseThrow(() -> new NotFoundException("confirmed_asset_not_found"));
    }

    private ProductPlanType resolveOffer(Product product, String collectionCode) {
        PlanType collection = collectionRepo.findByCode(collectionCode)
                .orElseThrow(() -> new NotFoundException("collection_not_found: " + collectionCode));

        return offerRepo.findByProduct_IdAndPlanType_Code(product.getId(), collectionCode)
                .orElseGet(() -> offerRepo.save(ProductPlanType.builder()
                        .product(product)
                        .planType(collection)
                        .priceCents(0)
                        .includedInBundle(false)
                        // Criado só para ancorar o arquivo: quem decide se vira oferta,
                        // e por quanto, é o administrador.
                        .available(false)
                        .sortOrder(900)
                        .build()));
    }

    private List<PresignedPart> presignParts(String key, String uploadId, int from, int to) {
        List<PresignedPart> parts = new ArrayList<>();

        for (int partNumber = from; partNumber <= to; partNumber++) {
            parts.add(new PresignedPart(
                    partNumber,
                    storage.presignUploadPart(key, uploadId, partNumber, multipartPresignTtl)
            ));
        }

        return parts;
    }

    private PendingUpload requirePending(UUID uploadId) {
        PendingUpload pending = pendingRepo.findById(uploadId)
                .orElseThrow(() -> new NotFoundException("upload_not_found"));

        if (pending.getStatus() != PendingUpload.Status.PENDING) {
            throw new ConflictException("upload_not_pending");
        }

        return pending;
    }

    private void validateSize(Long sizeBytes, boolean isMedia) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new ConflictException("invalid_size");
        }

        long limit = isMedia ? mediaMaxSizeBytes : maxSizeBytes;

        if (sizeBytes > limit) {
            throw new ConflictException("file_too_large");
        }
    }

    private void validateExtension(String filename, boolean isMedia) {
        Set<String> allowed = isMedia ? mediaExtensions : allowedExtensions;

        if (allowed.isEmpty()) {
            return; // sem lista configurada, não restringe
        }

        String extension = extensionOf(filename);

        if (extension.isBlank() || !allowed.contains(extension)) {
            throw new ConflictException("extension_not_allowed: " + extension);
        }
    }

    private String extensionOf(String value) {
        int dot = value.lastIndexOf('.');
        return dot < 0 || dot == value.length() - 1
                ? ""
                : value.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Set<String> parseSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
