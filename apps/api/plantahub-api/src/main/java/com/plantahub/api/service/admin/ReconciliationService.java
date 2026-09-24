package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.domain.ops.ReconciliationFinding;
import com.plantahub.api.domain.ops.ReconciliationFinding.Severity;
import com.plantahub.api.domain.ops.ReconciliationFinding.Type;
import com.plantahub.api.domain.ops.ReconciliationRun;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.PlanTypeRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.repository.ReconciliationFindingRepository;
import com.plantahub.api.repository.ReconciliationRunRepository;
import com.plantahub.api.shared.storage.ObjectStorageReader;
import com.plantahub.api.shared.storage.StorageKeyParser;
import com.plantahub.api.shared.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compara o bucket com o que o banco conhece e cria as linhas faltantes.
 *
 * <p>E o passo que transforma "o layout de pastas do S3 e o mapeamento compra para
 * arquivo" em "o banco e a fonte da verdade sobre quais arquivos existem". As chaves
 * gravadas sao <b>literalmente</b> as devolvidas pelo bucket, incluindo formatos antigos
 * como {@code .../ARCH/v1/planta.pdf}. Nenhum objeto e movido ou renomeado: mover
 * invalidaria URLs assinadas ja emitidas e nao traria beneficio nenhum.
 *
 * <p><b>Nunca apaga nada.</b> A garantia e estrutural, nao uma convencao: a dependencia
 * declarada e {@link ObjectStorageReader}, uma interface sem metodo de escrita ou remocao.
 * Divergencias viram achados para uma pessoa decidir.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ObjectStorageReader storage;
    private final ProductRepository productRepo;
    private final PlanTypeRepository planTypeRepo;
    private final ProductPlanTypeRepository pptRepo;
    private final DigitalAssetRepository assetRepo;
    private final ReconciliationRunRepository runRepo;
    private final ReconciliationFindingRepository findingRepo;

    public ReconciliationService(ObjectStorageReader storage,
                                 ProductRepository productRepo,
                                 PlanTypeRepository planTypeRepo,
                                 ProductPlanTypeRepository pptRepo,
                                 DigitalAssetRepository assetRepo,
                                 ReconciliationRunRepository runRepo,
                                 ReconciliationFindingRepository findingRepo) {
        this.storage = storage;
        this.productRepo = productRepo;
        this.planTypeRepo = planTypeRepo;
        this.pptRepo = pptRepo;
        this.assetRepo = assetRepo;
        this.runRepo = runRepo;
        this.findingRepo = findingRepo;
    }

    /**
     * Executa a varredura registrada em {@code runId}.
     *
     * <p>Idempotente: a unicidade de {@code digital_asset.storage_key} garante que uma
     * chave ja conhecida nunca vira linha nova. Executar dez vezes produz um unico estado.
     */
    @Transactional
    public void execute(UUID runId) {
        ReconciliationRun run = runRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("reconciliation_run_not_found"));

        boolean dryRun = Boolean.TRUE.equals(run.getDryRun());
        String scopedProductId = trimToNull(run.getProductId());

        // Varrer somente sob products/ ja exclui bundles/ e temp-downloads/, que sao
        // cache interno da aplicacao e nunca podem virar arquivo de catalogo.
        String prefix = scopedProductId != null
                ? StorageKeyParser.PRODUCTS_PREFIX + scopedProductId + "/"
                : StorageKeyParser.PRODUCTS_PREFIX;

        List<StoredObject> objects = storage.list(prefix);

        Scan scan = new Scan(run, dryRun);

        for (StoredObject object : objects) {
            scanObject(scan, object);
        }

        detectOrphans(scan, scopedProductId);
        detectDuplicateFilenames(scan, scopedProductId);

        findingRepo.saveAll(scan.findings);

        run.setObjectsScanned(objects.size());
        run.setAssetsCreated(scan.created);
        run.setAssetsUpdated(scan.updated);
        run.setAssetsMatched(scan.matched);
        run.setFindingsCount(scan.findings.size());
        run.setStatus(ReconciliationRun.Status.COMPLETED);
        run.setFinishedAt(Instant.now());
        runRepo.save(run);

        log.info("Reconciliacao {} concluida: {} objetos, {} criados, {} atualizados, "
                        + "{} ja conhecidos, {} achados (dryRun={})",
                runId, objects.size(), scan.created, scan.updated, scan.matched,
                scan.findings.size(), dryRun);
    }

    /**
     * Marca a execucao como falha numa transacao propria.
     *
     * <p>{@code REQUIRES_NEW} porque a transacao de {@link #execute} ja terá sofrido
     * rollback quando isto for chamado; sem uma transacao nova, o registro da falha seria
     * desfeito junto e a execucao ficaria eternamente em RUNNING.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID runId, String message) {
        runRepo.findById(runId).ifPresent(run -> {
            run.setStatus(ReconciliationRun.Status.FAILED);
            run.setErrorMessage(message);
            run.setFinishedAt(Instant.now());
            runRepo.save(run);
        });
    }

    // ------------------------------------------------------------------
    // Varredura de um objeto
    // ------------------------------------------------------------------

    private void scanObject(Scan scan, StoredObject object) {
        scan.seenKeys.add(object.key());

        var parsed = StorageKeyParser.parse(object.key());

        if (parsed.shape() == StorageKeyParser.Shape.UNPARSEABLE) {
            scan.add(Type.UNPARSEABLE_KEY, Severity.INFO, object.key(), null, null, null,
                    "Chave fora de qualquer formato reconhecido.");
            return;
        }

        Product product = scan.productCache.computeIfAbsent(
                parsed.productId(), id -> productRepo.findById(id).orElse(null));

        if (product == null) {
            scan.add(Type.UNKNOWN_PRODUCT, Severity.WARN, object.key(), parsed.productId(), null, null,
                    "Nenhum produto com o id '" + parsed.productId() + "'.");
            return;
        }

        if (parsed.shape() == StorageKeyParser.Shape.DIRECTLY_UNDER_PRODUCT) {
            if (parsed.isImage()) {
                // Sem esta regra a capa do produto viraria arquivo comprável, entregue no
                // download junto das plantas.
                scan.add(Type.MEDIA_CANDIDATE, Severity.INFO, object.key(), product.getId(), null, null,
                        "Imagem solta sob o produto. Candidata a midia de vitrine, "
                                + "nunca a arquivo entregue ao cliente.");
            } else {
                scan.add(Type.UNPARSEABLE_KEY, Severity.INFO, object.key(), product.getId(), null, null,
                        "Arquivo solto sob o produto, sem colecao.");
            }
            return;
        }

        String code = StorageKeyParser.normalizeFolder(parsed.folder());

        PlanType collection = scan.collectionCache.computeIfAbsent(
                code, c -> planTypeRepo.findByCode(c).orElse(null));

        if (collection == null) {
            scan.add(Type.UNKNOWN_COLLECTION, Severity.WARN, object.key(), product.getId(), code, null,
                    "A pasta '" + parsed.folder() + "' nao corresponde a nenhuma colecao. "
                            + "Crie a colecao com esse codigo e rode de novo.");
            return;
        }

        // A busca pelo asset vem ANTES de resolver o vinculo: se a linha ja existe, o
        // vinculo existe por consequencia (e chave estrangeira), entao consultar seria
        // trabalho jogado fora.
        DigitalAsset existing = assetRepo.findByStorageKey(object.key()).orElse(null);

        if (existing != null) {
            if (!isMetadataMissing(existing, object)) {
                scan.matched++;
                return;
            }

            scan.updated++;

            // Em dry run nem sequer tocamos na entidade: ela e gerenciada pelo JPA, entao
            // qualquer alteracao seria gravada no flush e o "ensaio" viraria escrita.
            if (!scan.dryRun) {
                fillMissingMetadata(existing, object);
            }

            scan.add(Type.METADATA_UPDATED, Severity.INFO, object.key(),
                    product.getId(), collection.getCode(), existing.getId(),
                    scan.dryRun
                            ? "Metadado ausente seria preenchido a partir do bucket."
                            : "Metadado ausente preenchido a partir do bucket.");
            return;
        }

        ProductPlanType offer = resolveOffer(scan, product, collection, object.key());

        if (scan.dryRun) {
            // Em ensaio o vinculo pode ainda nao existir (offer == null). Mesmo assim o
            // arquivo conta como "seria criado": numa execucao real o vinculo nasce junto.
            // Omitir isto faria o relatorio subnotificar exatamente os casos novos.
            scan.created++;
            scan.add(Type.CREATED, Severity.INFO, object.key(),
                    product.getId(), collection.getCode(), null, "Seria criado (dry run).");
            return;
        }

        DigitalAsset asset = assetRepo.save(buildAsset(offer, object, parsed));
        scan.created++;

        scan.add(Type.CREATED, Severity.INFO, object.key(),
                product.getId(), collection.getCode(), asset.getId(),
                "Linha criada apontando para a chave literal do bucket.");
    }

    private ProductPlanType resolveOffer(Scan scan, Product product, PlanType collection, String key) {
        String cacheKey = product.getId() + "::" + collection.getCode();

        ProductPlanType cached = scan.offerCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        ProductPlanType offer = pptRepo
                .findByProduct_IdAndPlanType_Code(product.getId(), collection.getCode())
                .orElse(null);

        if (offer == null) {
            scan.add(Type.MISSING_PRODUCT_PLAN_TYPE, Severity.INFO, key,
                    product.getId(), collection.getCode(), null,
                    "Produto nao tinha vinculo com esta colecao. "
                            + (scan.dryRun ? "Seria criado." : "Vinculo criado."));

            if (scan.dryRun) {
                return null;
            }

            offer = pptRepo.save(ProductPlanType.builder()
                    .product(product)
                    .planType(collection)
                    .priceCents(0)
                    .includedInBundle(false)
                    // Vinculo criado por reconciliacao nunca nasce a venda: quem decide se
                    // isto e uma oferta, e por quanto, e o admin.
                    .available(false)
                    .sortOrder(900)
                    .build());
        }

        scan.offerCache.put(cacheKey, offer);
        return offer;
    }

    private DigitalAsset buildAsset(ProductPlanType offer,
                                    StoredObject object,
                                    StorageKeyParser.ParsedKey parsed) {
        String extension = parsed.extension();

        return DigitalAsset.builder()
                .productPlanType(offer)
                .version(1)
                .filename(parsed.filename())
                // A chave literal, sem reinterpretacao.
                .storageKey(object.key())
                .sizeBytes(object.sizeBytes())
                .etag(object.eTag())
                .fileExt(extension.isBlank() ? null : extension)
                .mediaType(object.contentType())
                .relativePath(parsed.relativePath())
                .kind("FILE")
                .keyScheme("LEGACY")
                .reconciliationStatus("MATCHED")
                .sortOrder(0)
                .createdAt(object.lastModified() != null ? object.lastModified() : Instant.now())
                .build();
    }

    private boolean isMetadataMissing(DigitalAsset asset, StoredObject object) {
        return (asset.getSizeBytes() == null && object.sizeBytes() != null)
                || (asset.getEtag() == null && object.eTag() != null)
                || (asset.getMediaType() == null && object.contentType() != null)
                || !"MATCHED".equals(asset.getReconciliationStatus());
    }

    private void fillMissingMetadata(DigitalAsset asset, StoredObject object) {
        if (asset.getSizeBytes() == null && object.sizeBytes() != null) {
            asset.setSizeBytes(object.sizeBytes());
        }
        if (asset.getEtag() == null && object.eTag() != null) {
            asset.setEtag(object.eTag());
        }
        if (asset.getMediaType() == null && object.contentType() != null) {
            asset.setMediaType(object.contentType());
        }
        asset.setReconciliationStatus("MATCHED");
    }

    /**
     * Linhas cuja chave nao existe mais no bucket.
     *
     * <p>Apenas registradas e marcadas. Apagar automaticamente poderia remover o direito de
     * download de alguem que ja pagou por causa de uma listagem incompleta ou de um erro
     * transitorio do bucket.
     */
    private void detectOrphans(Scan scan, String scopedProductId) {
        for (DigitalAsset asset : loadAssets(scopedProductId)) {
            if (scan.seenKeys.contains(asset.getStorageKey())) {
                continue;
            }

            // Confirma com o bucket antes de acusar: a chave pode estar fora do prefixo varrido.
            if (storage.head(asset.getStorageKey()).isPresent()) {
                continue;
            }

            if (!scan.dryRun) {
                asset.setReconciliationStatus("MISSING_OBJECT");
            }

            scan.add(Type.ORPHAN_DB, Severity.WARN, asset.getStorageKey(),
                    asset.getProductPlanType().getProduct().getId(),
                    asset.getProductPlanType().getPlanType().getCode(),
                    asset.getId(),
                    "Linha no banco sem objeto correspondente no bucket. Nao foi apagada.");
        }
    }

    private void detectDuplicateFilenames(Scan scan, String scopedProductId) {
        Map<String, List<DigitalAsset>> byOfferAndName = new HashMap<>();

        for (DigitalAsset asset : loadAssets(scopedProductId)) {
            if (asset.getDeletedAt() != null) {
                continue;
            }
            String key = asset.getProductPlanType().getId() + "::"
                    + asset.getFilename().toLowerCase(Locale.ROOT);
            byOfferAndName.computeIfAbsent(key, k -> new ArrayList<>()).add(asset);
        }

        byOfferAndName.values().stream()
                .filter(group -> group.size() > 1)
                .forEach(group -> {
                    DigitalAsset first = group.get(0);
                    scan.add(Type.DUPLICATE_FILENAME, Severity.INFO, first.getStorageKey(),
                            first.getProductPlanType().getProduct().getId(),
                            first.getProductPlanType().getPlanType().getCode(),
                            first.getId(),
                            group.size() + " arquivos vivos com o nome '" + first.getFilename()
                                    + "' na mesma colecao.");
                });
    }

    private List<DigitalAsset> loadAssets(String scopedProductId) {
        return scopedProductId != null
                ? assetRepo.findByProductId(scopedProductId)
                : assetRepo.findAllWithRelations();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Estado mutavel de uma varredura, para nao passar dez parametros por metodo. */
    private final class Scan {
        final ReconciliationRun run;
        final boolean dryRun;

        final List<ReconciliationFinding> findings = new ArrayList<>();
        final Set<String> seenKeys = new HashSet<>();
        final Map<String, Product> productCache = new HashMap<>();
        final Map<String, PlanType> collectionCache = new HashMap<>();
        final Map<String, ProductPlanType> offerCache = new HashMap<>();

        int created;
        int updated;
        int matched;

        Scan(ReconciliationRun run, boolean dryRun) {
            this.run = run;
            this.dryRun = dryRun;
        }

        void add(Type type, Severity severity, String storageKey, String productId,
                 String planTypeCode, UUID assetId, String detail) {
            findings.add(ReconciliationFinding.builder()
                    .run(run)
                    .type(type)
                    .severity(severity)
                    .storageKey(storageKey)
                    .productId(productId)
                    .planTypeCode(planTypeCode)
                    .digitalAssetId(assetId)
                    .detail(detail)
                    .build());
        }
    }
}
