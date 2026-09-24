package com.plantahub.api.reconciliation;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.ops.ReconciliationFinding.Type;
import com.plantahub.api.domain.ops.ReconciliationRun;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ReconciliationFindingRepository;
import com.plantahub.api.repository.ReconciliationRunRepository;
import com.plantahub.api.service.admin.ReconciliationService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A reconciliacao e a peca de maior risco da modernizacao: ela decide o que o banco vai
 * afirmar sobre arquivos que ja existem e ja foram vendidos.
 *
 * <p>O bucket simulado aqui reproduz de proposito as situacoes reais do bucket do
 * PlantaHUB: chave legada com segmento de versao, capa de produto solta, pasta que nao
 * corresponde a colecao nenhuma, produto inexistente e arquivos homonimos.
 */
@Transactional
class ReconciliationServiceTest extends AbstractApiTest {

    @Autowired private ReconciliationService service;
    @Autowired private ReconciliationRunRepository runRepo;
    @Autowired private ReconciliationFindingRepository findingRepo;
    @Autowired private DigitalAssetRepository assetRepo;
    @Autowired private ProductPlanTypeRepository pptRepo;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    private Product product;
    private String arch;
    private String apoio;

    @BeforeEach
    void setUp() {
        storage.clear();

        product = fixtures.product(TestDataFactory.unique("prod"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        arch = TestDataFactory.uniqueCode("ARCH");
        apoio = TestDataFactory.uniqueCode("APOIO");

        fixtures.collection(arch);
        fixtures.collection(apoio, false, true);
    }

    private UUID startRun(boolean dryRun) {
        return runRepo.save(ReconciliationRun.builder()
                .dryRun(dryRun)
                .productId(product.getId())
                .status(ReconciliationRun.Status.RUNNING)
                .build()).getId();
    }

    private List<Type> findingTypes(UUID runId) {
        return findingRepo.findByRun_IdOrderByTypeAsc(runId).stream()
                .map(f -> f.getType())
                .toList();
    }

    private String key(String suffix) {
        return "products/" + product.getId() + "/" + suffix;
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("dry run nao escreve linha nenhuma em digital_asset")
    void dryRunWritesNothing() {
        storage.seed(key(arch + "/planta.pdf"), "conteudo");

        UUID runId = startRun(true);
        service.execute(runId);

        assertThat(assetRepo.findByProductId(product.getId()))
                .as("um ensaio nao pode criar dados")
                .isEmpty();

        var run = runRepo.findById(runId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.Status.COMPLETED);
        assertThat(run.getAssetsCreated()).isEqualTo(1);
        assertThat(findingTypes(runId)).contains(Type.CREATED);
    }

    @Test
    @DisplayName("grava a chave LITERAL do bucket, inclusive o formato legado com versao")
    void preservesLegacyKeysVerbatim() {
        String legacyKey = key(arch + "/v1/planta-arquitetonica.pdf");
        storage.seed(legacyKey, "conteudo");

        service.execute(startRun(false));

        var assets = assetRepo.findByProductId(product.getId());

        assertThat(assets).hasSize(1);
        assertThat(assets.get(0).getStorageKey())
                .as("mover ou reescrever chaves invalidaria URLs assinadas ja emitidas")
                .isEqualTo(legacyKey);
        assertThat(assets.get(0).getKeyScheme()).isEqualTo("LEGACY");
        assertThat(assets.get(0).getFilename()).isEqualTo("planta-arquitetonica.pdf");
        assertThat(assets.get(0).getRelativePath()).isEqualTo("v1");
        assertThat(assets.get(0).getFileExt()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("a capa do produto vira candidata a midia, nunca arquivo comprável")
    void productCoverNeverBecomesADownloadableAsset() {
        storage.seed(key("cover.webp"), "imagem");
        storage.seed(key(arch + "/planta.pdf"), "conteudo");

        UUID runId = startRun(false);
        service.execute(runId);

        assertThat(assetRepo.findByProductId(product.getId()))
                .as("sem esta regra a capa seria entregue no download junto das plantas")
                .extracting(a -> a.getFilename())
                .containsExactly("planta.pdf");

        assertThat(findingTypes(runId)).contains(Type.MEDIA_CANDIDATE);
    }

    @Test
    @DisplayName("pasta sem colecao correspondente e reportada, nao adivinhada")
    void unknownFolderIsReported() {
        storage.seed(key("PASTA_DESCONHECIDA/arquivo.pdf"), "conteudo");

        UUID runId = startRun(false);
        service.execute(runId);

        assertThat(assetRepo.findByProductId(product.getId())).isEmpty();
        assertThat(findingTypes(runId)).contains(Type.UNKNOWN_COLLECTION);
    }

    @Test
    @DisplayName("cria o vinculo produto-colecao que faltava")
    void createsMissingProductPlanType() {
        storage.seed(key(apoio + "/memorial.pdf"), "conteudo");

        assertThat(pptRepo.findByProduct_IdAndPlanType_Code(product.getId(), apoio)).isEmpty();

        UUID runId = startRun(false);
        service.execute(runId);

        var offer = pptRepo.findByProduct_IdAndPlanType_Code(product.getId(), apoio).orElseThrow();

        assertThat(offer.getAvailable())
                .as("vinculo criado por reconciliacao nao pode nascer a venda")
                .isFalse();
        assertThat(findingTypes(runId)).contains(Type.MISSING_PRODUCT_PLAN_TYPE);
        assertThat(assetRepo.findByProductId(product.getId())).hasSize(1);
    }

    @Test
    @DisplayName("linha sem objeto no bucket e marcada, nunca apagada")
    void orphanRowsAreFlaggedNotDeleted() {
        var offer = fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("OLD")), 0);
        var orphan = fixtures.asset(offer, "sumiu.pdf");

        UUID runId = startRun(false);
        service.execute(runId);

        assertThat(assetRepo.findById(orphan.getId()))
                .as("apagar aqui poderia tirar o download de quem ja pagou")
                .isPresent();
        assertThat(orphan.getReconciliationStatus()).isEqualTo("MISSING_OBJECT");
        assertThat(findingTypes(runId)).contains(Type.ORPHAN_DB);
    }

    @Test
    @DisplayName("arquivos homonimos na mesma colecao sao reportados")
    void duplicateFilenamesAreReported() {
        storage.seed(key(arch + "/pasta-a/planta.pdf"), "a");
        storage.seed(key(arch + "/pasta-b/planta.pdf"), "b");

        UUID runId = startRun(false);
        service.execute(runId);

        assertThat(assetRepo.findByProductId(product.getId()))
                .as("nomes iguais em subpastas diferentes sao legitimos, so precisam ser visiveis")
                .hasSize(2);
        assertThat(findingTypes(runId)).contains(Type.DUPLICATE_FILENAME);
    }

    @Test
    @DisplayName("tres execucoes produzem exatamente o mesmo estado")
    void isIdempotent() {
        storage.seed(key(arch + "/planta.pdf"), "conteudo");
        storage.seed(key(apoio + "/memorial.pdf"), "conteudo");

        service.execute(startRun(false));
        var afterFirst = assetRepo.findByProductId(product.getId()).stream()
                .map(a -> a.getStorageKey()).sorted().toList();

        service.execute(startRun(false));
        UUID thirdRun = startRun(false);
        service.execute(thirdRun);

        var afterThird = assetRepo.findByProductId(product.getId()).stream()
                .map(a -> a.getStorageKey()).sorted().toList();

        assertThat(afterThird).isEqualTo(afterFirst).hasSize(2);

        var run = runRepo.findById(thirdRun).orElseThrow();
        assertThat(run.getAssetsCreated()).isZero();
        assertThat(run.getAssetsMatched()).isEqualTo(2);
    }

    @Test
    @DisplayName("o cache interno de ZIPs fica fora da varredura")
    void bundleCacheIsNeverScanned() {
        storage.seed("bundles/abc123/plantahub-bundle-abc123.zip", "zip");
        storage.seed("private/bundles/def456/outro.zip", "zip");
        storage.seed("temp-downloads/alguem@exemplo.com/arquivo.zip", "zip");
        storage.seed(key(arch + "/planta.pdf"), "conteudo");

        UUID runId = startRun(false);
        service.execute(runId);

        assertThat(assetRepo.findByProductId(product.getId()))
                .as("ZIP de cache nunca pode virar arquivo de catalogo")
                .hasSize(1);

        var run = runRepo.findById(runId).orElseThrow();
        assertThat(run.getObjectsScanned()).isEqualTo(1);
    }

    @Test
    @DisplayName("completa a execucao mesmo com o bucket vazio")
    void emptyBucketCompletes() {
        UUID runId = startRun(false);
        service.execute(runId);

        var run = runRepo.findById(runId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(ReconciliationRun.Status.COMPLETED);
        assertThat(run.getObjectsScanned()).isZero();
    }
}
