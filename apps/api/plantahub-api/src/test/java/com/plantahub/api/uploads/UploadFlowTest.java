package com.plantahub.api.uploads;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.uploads.PendingUpload;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.PendingUploadRepository;
import com.plantahub.api.service.admin.UploadService;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O upload vai do navegador direto ao bucket; a API so assina e registra.
 *
 * <p>Por isso o ponto central destes testes e um so: <b>nada do que o cliente afirma sobre
 * o arquivo e aceito</b>. Tamanho e tipo vem do proprio bucket, e a chave de destino e
 * escolhida pelo servidor antes de os bytes existirem.
 */
@Transactional
class UploadFlowTest extends AbstractApiTest {

    @Autowired private UploadService uploadService;
    @Autowired private PendingUploadRepository pendingRepo;
    @Autowired private DigitalAssetRepository assetRepo;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    private String adminEmail;
    private Product product;
    private String collectionCode;

    @BeforeEach
    void setUp() {
        storage.clear();

        adminEmail = fixtures.user().getEmail();
        product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        collectionCode = TestDataFactory.uniqueCode("ARCH");
        var collection = fixtures.collection(collectionCode);
        fixtures.offer(product, collection, 150000);
    }

    private PresignRequest request(String filename, long sizeBytes) {
        return new PresignRequest(PendingUpload.TargetKind.ASSET, product.getId(), collectionCode,
                filename, "application/pdf", sizeBytes, null);
    }

    @Test
    @DisplayName("a chave e escolhida pelo servidor, com o nome normalizado")
    void serverChoosesTheKey() {
        var response = uploadService.presign(adminEmail, request("Planta Arquitetônica.pdf", 1024L));

        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.url()).isNotBlank();
        assertThat(response.storageKey())
                .startsWith("products/" + product.getId() + "/" + collectionCode + "/")
                .endsWith("/planta-arquitetonica.pdf");

        // Gravada antes de os bytes existirem: e o que impede a confirmacao de ser forjada
        // para apontar a um objeto arbitrario do bucket.
        var pending = pendingRepo.findById(response.uploadId()).orElseThrow();
        assertThat(pending.getStorageKey()).isEqualTo(response.storageKey());
        assertThat(pending.getStatus()).isEqualTo(PendingUpload.Status.PENDING);
    }

    @Test
    @DisplayName("o tamanho gravado vem do bucket, nao do que o cliente declarou")
    void sizeComesFromTheBucket() {
        // O cliente diz 5 bytes...
        var response = uploadService.presign(adminEmail, request("planta.pdf", 5L));

        // ...mas o que foi realmente gravado tem outro tamanho.
        byte[] realContent = "conteudo bem maior do que cinco bytes".getBytes();
        storage.seed(response.storageKey(), realContent, "application/pdf");

        var assetId = uploadService.confirm(response.uploadId(), null);
        var asset = assetRepo.findById(assetId).orElseThrow();

        assertThat(asset.getSizeBytes())
                .as("confiar no tamanho declarado permitiria registrar metadados inventados")
                .isEqualTo((long) realContent.length);
    }

    @Test
    @DisplayName("confirmar sem o objeto no bucket e recusado")
    void confirmWithoutObjectFails() {
        var response = uploadService.presign(adminEmail, request("planta.pdf", 1024L));

        // Nada foi enviado: o navegador caiu antes do PUT.
        assertThatThrownBy(() -> uploadService.confirm(response.uploadId(), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("upload_object_missing");

        // O registro segue pendente, entao o cliente pode reenviar e confirmar de novo.
        assertThat(pendingRepo.findById(response.uploadId()).orElseThrow().getStatus())
                .isEqualTo(PendingUpload.Status.PENDING);
    }

    @Test
    @DisplayName("confirmar duas vezes nao duplica o arquivo")
    void confirmIsIdempotent() {
        var response = uploadService.presign(adminEmail, request("planta.pdf", 1024L));
        storage.seed(response.storageKey(), "conteudo");

        var firstId = uploadService.confirm(response.uploadId(), null);
        var secondId = uploadService.confirm(response.uploadId(), null);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(assetRepo.findByProductId(product.getId())).hasSize(1);
    }

    @Test
    @DisplayName("o arquivo registrado guarda a chave nova e o nome original")
    void recordKeepsBothNames() {
        var response = uploadService.presign(adminEmail, request("Planta Arquitetônica.pdf", 1024L));
        storage.seed(response.storageKey(), "conteudo");

        var asset = assetRepo.findById(uploadService.confirm(response.uploadId(), null)).orElseThrow();

        // Nome de exibicao: o original, com acento. Chave: a versao normalizada.
        assertThat(asset.getFilename()).isEqualTo("Planta Arquitetônica.pdf");
        assertThat(asset.getStorageKey()).endsWith("/planta-arquitetonica.pdf");
        assertThat(asset.getKeyScheme()).isEqualTo("V2");
        assertThat(asset.getFileExt()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("arquivo grande vai por multipart, com as partes assinadas")
    void largeFileUsesMultipart() {
        // O limite do perfil de teste e 1 MB.
        var response = uploadService.presign(adminEmail, request("planta.dwg", 3L * 1024 * 1024));

        assertThat(response.method()).isEqualTo("MULTIPART");
        assertThat(response.multipartUploadId()).isNotBlank();
        assertThat(response.parts()).isNotEmpty();
        assertThat(response.parts().get(0).partNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("abortar cancela o multipart no bucket")
    void abortCancelsMultipart() {
        var response = uploadService.presign(adminEmail, request("planta.dwg", 3L * 1024 * 1024));

        uploadService.abort(response.uploadId());

        assertThat(storage.wasAborted(response.multipartUploadId()))
                .as("partes orfas continuam sendo cobradas ate serem abortadas")
                .isTrue();
        assertThat(pendingRepo.findById(response.uploadId()).orElseThrow().getStatus())
                .isEqualTo(PendingUpload.Status.ABORTED);
    }

    @Test
    @DisplayName("extensao fora da lista permitida e recusada")
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() -> uploadService.presign(adminEmail, request("script.exe", 1024L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("extension_not_allowed");
    }

    @Test
    @DisplayName("arquivo acima do limite e recusado antes de qualquer byte trafegar")
    void rejectsOversizedFile() {
        assertThatThrownBy(() ->
                uploadService.presign(adminEmail, request("planta.pdf", 999L * 1024 * 1024)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("file_too_large");
    }

    @Test
    @DisplayName("produto arquivado nao recebe upload")
    void rejectsArchivedProduct() {
        product.applyStatus(ProductStatus.ARCHIVED);

        assertThatThrownBy(() -> uploadService.presign(adminEmail, request("planta.pdf", 1024L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("product_archived");
    }

    @Test
    @DisplayName("upload de pasta preserva a subpasta na chave e no registro")
    void folderUploadKeepsSubpath() {
        var response = uploadService.presign(adminEmail, new PresignRequest(
                PendingUpload.TargetKind.ASSET, product.getId(), collectionCode,
                "Prancha 01.pdf", "application/pdf", 1024L, "Pranchas/Nível 1"));

        storage.seed(response.storageKey(), "conteudo");

        var asset = assetRepo.findById(uploadService.confirm(response.uploadId(), null)).orElseThrow();

        assertThat(asset.getRelativePath()).isEqualTo("pranchas/nivel-1");
        assertThat(response.storageKey()).contains("/pranchas/nivel-1/");
    }

    @Test
    @DisplayName("coleção sem vínculo com o produto ganha o vínculo, fora de venda")
    void createsMissingOffer() {
        String newCode = TestDataFactory.uniqueCode("NOVA");
        fixtures.collection(newCode);

        var response = uploadService.presign(adminEmail, new PresignRequest(
                PendingUpload.TargetKind.ASSET, product.getId(), newCode,
                "planta.pdf", "application/pdf", 1024L, null));

        storage.seed(response.storageKey(), "conteudo");

        var asset = assetRepo.findById(uploadService.confirm(response.uploadId(), null)).orElseThrow();

        assertThat(asset.getProductPlanType().getAvailable())
                .as("o vinculo nasce so para ancorar o arquivo; virar oferta e decisao do admin")
                .isFalse();
    }
}
