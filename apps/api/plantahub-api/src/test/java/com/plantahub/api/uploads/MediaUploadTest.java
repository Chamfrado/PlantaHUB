package com.plantahub.api.uploads;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.uploads.PendingUpload;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.service.admin.AdminMediaService;
import com.plantahub.api.service.admin.AdminProductService;
import com.plantahub.api.service.admin.UploadService;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.AdminMediaDTOs.UpdateMediaRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Capa e galeria do produto.
 *
 * <p>Mídia se separa de arquivo comprável em duas coisas que este teste protege:
 *
 * <ul>
 *   <li>a chave nasce sob {@code public/}, fora do prefixo onde vivem os arquivos que só
 *       quem comprou pode ler — assim a política do bucket que libera leitura anônima
 *       nunca precisa alcançar {@code products/};</li>
 *   <li>a linha nasce com URL absoluta. Sem ela, a vitrine receberia a chave do bucket como
 *       {@code src} e o navegador a resolveria contra a origem do próprio site.</li>
 * </ul>
 */
@Transactional
class MediaUploadTest extends AbstractApiTest {

    @Autowired private UploadService uploadService;
    @Autowired private AdminMediaService mediaService;
    @Autowired private AdminProductService productService;
    @Autowired private ProductCatalogService catalogService;
    @Autowired private ProductMediaRepository mediaRepo;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    private String adminEmail;
    private Product product;

    @BeforeEach
    void setUp() {
        storage.clear();

        adminEmail = fixtures.user().getEmail();
        product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);
    }

    private PresignRequest mediaRequest(String filename, long sizeBytes) {
        return new PresignRequest(PendingUpload.TargetKind.MEDIA, product.getId(), null,
                filename, "image/webp", sizeBytes, null);
    }

    /** Sobe uma imagem de ponta a ponta e devolve o id da linha criada. */
    private UUID upload(String filename) {
        var presigned = uploadService.presign(adminEmail, mediaRequest(filename, 2048L));
        storage.seed(presigned.storageKey(), new byte[2048], "image/webp");
        return uploadService.confirm(presigned.uploadId(), null);
    }

    @Test
    @DisplayName("a imagem vai para o prefixo publico, longe dos arquivos vendidos")
    void mediaLivesUnderThePublicPrefix() {
        var presigned = uploadService.presign(adminEmail, mediaRequest("Fachada Frontal.webp", 2048L));

        assertThat(presigned.storageKey())
                .as("a politica de leitura anonima do bucket e escrita sobre este prefixo")
                .startsWith("public/products/" + product.getId() + "/")
                .endsWith("/fachada-frontal.webp");

        assertThat(presigned.storageKey())
                .as("um curinga no meio de products/ seria alargado para products/* no dia "
                        + "em que uma imagem aparecesse quebrada, publicando o acervo junto")
                .doesNotStartWith("products/");
    }

    @Test
    @DisplayName("a linha nasce com URL absoluta, e nao com a chave do bucket")
    void mediaRowCarriesAnAbsoluteUrl() {
        UUID mediaId = upload("fachada.webp");

        ProductMedia media = mediaRepo.findById(mediaId).orElseThrow();

        assertThat(media.getPublicUrl())
                .startsWith("https://")
                .endsWith("/" + media.getStorageKey());

        assertThat(media.getSizeBytes())
                .as("o tamanho vem do HeadObject, nao do que o cliente declarou")
                .isEqualTo(2048L);
    }

    @Test
    @DisplayName("a segunda imagem entra na galeria, sem disputar a capa")
    void everyUploadStartsInTheGallery() {
        UUID first = upload("fachada.webp");
        UUID second = upload("planta-baixa.webp");
        UUID third = upload("area-gourmet.webp");

        assertThat(mediaService.list(product.getId()))
                .as("promover a capa e acao explicita: trocar a vitrine nao pode ser efeito "
                        + "colateral de arrastar um arquivo para a tela")
                .extracting(ProductMedia::getRole)
                .containsOnly(ProductMedia.Role.GALLERY)
                .hasSize(3);

        assertThat(List.of(first, second, third)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("a galeria publica traz as imagens que nao sao capa, na ordem do painel")
    void publicGalleryFollowsTheAdminOrder() {
        UUID cover = upload("fachada.webp");
        UUID second = upload("planta-baixa.webp");
        UUID third = upload("area-gourmet.webp");

        mediaService.update(cover, new UpdateMediaRequest(ProductMedia.Role.HERO, null, null));
        mediaService.reorder(product.getId(), List.of(cover, third, second));

        var detail = catalogService.assembleDetail(productService.get(product.getId()));

        assertThat(detail.heroImageUrl())
                .as("a capa alimenta o cache de leitura que quatro DTOs consultam")
                .isEqualTo(mediaRepo.findById(cover).orElseThrow().getPublicUrl());

        assertThat(detail.galleryImageUrls())
                .as("a capa nao se repete na galeria, e a ordem e a que o admin definiu")
                .containsExactly(
                        mediaRepo.findById(third).orElseThrow().getPublicUrl(),
                        mediaRepo.findById(second).orElseThrow().getPublicUrl());
    }

    @Test
    @DisplayName("subir a capa destrava a publicacao")
    void uploadingACoverUnblocksPublishing() {
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 150000);
        productService.replaceContent(product.getId(), TestDataFactory.contentWithHeadline("Casa"));

        assertThat(productService.publishProblems(productService.get(product.getId())))
                .contains("missing_hero_image");

        UUID cover = upload("fachada.webp");
        mediaService.update(cover, new UpdateMediaRequest(ProductMedia.Role.HERO, null, null));

        assertThat(productService.publishProblems(productService.get(product.getId())))
                .as("era o unico impedimento restante")
                .isEmpty();
    }

    @Test
    @DisplayName("remover a ultima capa tira a imagem da vitrine e volta a barrar a publicacao")
    void deletingTheCoverClearsTheStorefrontImage() {
        UUID cover = upload("fachada.webp");
        mediaService.update(cover, new UpdateMediaRequest(ProductMedia.Role.HERO, null, null));

        mediaService.delete(cover);

        assertThat(productService.get(product.getId()).getHeroImageUrl())
                .as("deixar a URL de uma imagem removida faria a vitrine apontar para o nada")
                .isNull();

        assertThat(productService.publishProblems(productService.get(product.getId())))
                .contains("missing_hero_image");
    }

    @Test
    @DisplayName("arquivo que nao e imagem, ou grande demais, e recusado antes de subir")
    void mediaHasItsOwnLimits() {
        assertThatThrownBy(() -> uploadService.presign(adminEmail, mediaRequest("planta.pdf", 2048L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("extension_not_allowed");

        // O limite de midia e muito menor que o de arquivo: uma capa de 500 MB e engano,
        // e descobrir isso depois do upload desperdicaria a transferencia inteira.
        assertThatThrownBy(() -> uploadService.presign(adminEmail, mediaRequest("fachada.webp", 50L * 1024 * 1024)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("file_too_large");
    }
}
