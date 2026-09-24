package com.plantahub.api.storage;

import com.plantahub.api.domain.catalog.ProductMedia;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.service.admin.StorageDiagnosticsService;
import com.plantahub.api.shared.storage.BucketInspectionPort.CorsRule;
import com.plantahub.api.shared.storage.PublicMediaUrls;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.FakeBucketInspection;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.Check;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.DiagnosticsReport;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O diagnóstico do bucket.
 *
 * <p>Todo item verificado aqui corresponde a uma falha que <b>não</b> se anuncia: o upload
 * que sobe inteiro e nunca fecha, a parte órfã cobrada para sempre, o acervo pago aberto ao
 * mundo. O valor da tela está em transformar cada uma numa linha vermelha antes de alguém
 * descobrir do jeito difícil.
 */
@Transactional
class StorageDiagnosticsTest extends AbstractApiTest {

    @Autowired private StorageDiagnosticsService diagnostics;
    @Autowired private FakeBucketInspection bucket;
    @Autowired private PublicMediaUrls publicUrls;
    @Autowired private ProductMediaRepository mediaRepo;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        bucket.reset();
    }

    private Check check(DiagnosticsReport report, String id) {
        return report.checks().stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "verificação ausente: " + id + " (presentes: "
                                + report.checks().stream().map(Check::id).toList() + ")"));
    }

    /** Cria uma imagem no prefixo público, que é a amostra do teste de leitura anônima. */
    private String seedPublicMedia() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.DRAFT);

        String key = "public/products/" + product.getId() + "/" + java.util.UUID.randomUUID() + "/capa.webp";

        mediaRepo.save(ProductMedia.builder()
                .product(product)
                .role(ProductMedia.Role.GALLERY)
                .storageKey(key)
                .publicUrl(publicUrls.urlFor(key))
                .sortOrder(0)
                .build());

        return publicUrls.urlFor(key);
    }

    @Test
    @DisplayName("bucket saudavel passa em tudo que da para verificar")
    void healthyBucketPasses() {
        String url = seedPublicMedia();
        bucket.anonymousSees(url, 206);

        var report = diagnostics.run();

        assertThat(check(report, "bucket_reachable").status()).isEqualTo(Status.OK);
        assertThat(check(report, "region_matches").status()).isEqualTo(Status.OK);
        assertThat(check(report, "cors_expose_etag").status()).isEqualTo(Status.OK);
        assertThat(check(report, "cors_methods").status()).isEqualTo(Status.OK);
        assertThat(check(report, "lifecycle_abort_multipart").status()).isEqualTo(Status.OK);
        assertThat(check(report, "write_permission").status()).isEqualTo(Status.OK);
        assertThat(check(report, "public_prefix_readable").status()).isEqualTo(Status.OK);

        assertThat(report.checks())
                .as("nenhuma falha num bucket configurado corretamente")
                .noneMatch(c -> c.status() == Status.FAIL);
    }

    @Test
    @DisplayName("CORS sem ExposeHeaders ETag e reprovado, e o texto diz o que quebra")
    void missingExposeEtagIsReported() {
        bucket.withCors(new CorsRule(
                List.of("http://localhost:5173"),
                List.of("PUT", "POST", "GET", "HEAD"),
                List.of("*"),
                List.of()));

        var etag = check(diagnostics.run(), "cors_expose_etag");

        assertThat(etag.status()).isEqualTo(Status.FAIL);
        assertThat(etag.impact())
                .as("o sintoma e um upload grande que sobe inteiro e nunca termina; sem "
                        + "explicar isso, a linha vermelha nao ajuda ninguem")
                .contains("multipart");
    }

    @Test
    @DisplayName("bucket sem CORS nenhum reprova de uma vez so")
    void noCorsAtAll() {
        bucket.withoutCors();

        var report = diagnostics.run();

        assertThat(check(report, "cors").status()).isEqualTo(Status.FAIL);
    }

    @Test
    @DisplayName("origem do site fora do CORS e reprovada")
    void uncoveredOriginIsReported() {
        bucket.withCors(new CorsRule(
                List.of("https://outro-dominio.com"),
                List.of("PUT", "POST", "GET", "HEAD"),
                List.of("*"),
                List.of("ETag")));

        assertThat(check(diagnostics.run(), "cors_origins").status()).isEqualTo(Status.FAIL);
    }

    @Test
    @DisplayName("lifecycle ausente e aviso, nao falha: funciona, mas custa dinheiro")
    void missingLifecycleIsAWarning() {
        bucket.withoutLifecycle();

        var lifecycle = check(diagnostics.run(), "lifecycle_abort_multipart");

        assertThat(lifecycle.status())
                .as("o upload continua funcionando; o que nao funciona e a conta no fim do mes")
                .isEqualTo(Status.WARN);
        assertThat(lifecycle.impact()).contains("cobradas");
    }

    @Test
    @DisplayName("regiao divergente e falha, porque toda URL assinada quebra")
    void regionMismatchIsAFailure() {
        bucket.inRegion("sa-east-1");

        var region = check(diagnostics.run(), "region_matches");

        assertThat(region.status()).isEqualTo(Status.FAIL);
        assertThat(region.detail()).contains("sa-east-1");
    }

    @Test
    @DisplayName("arquivo vendido legivel sem login e a falha mais grave da tela")
    void publiclyReadablePaidFileIsAFailure() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var offer = fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 1000);
        fixtures.asset(offer, "planta.pdf");

        // Um bucket aberto responde a qualquer um, inclusive no prefixo pago.
        bucket.anonymousSeesEverything(200);

        var paid = check(diagnostics.run(), "paid_prefix_private");

        assertThat(paid.status()).isEqualTo(Status.FAIL);

        // Qual arquivo virou amostra nao importa; que a mensagem nomeie um arquivo pago,
        // sim — sem isso o operador nao sabe o que foi testado.
        assertThat(paid.detail()).contains("products/").contains("HTTP 200");
        assertThat(paid.impact())
                .as("o ponto e que a regra de entitlement vira decoracao")
                .contains("entitlement");
    }

    @Test
    @DisplayName("sem amostra, a verificacao fica desconhecida em vez de mentir que esta OK")
    void missingSampleIsUnknown() {
        var report = diagnostics.run();

        assertThat(check(report, "public_prefix_readable").status())
                .as("aprovar sem ter testado nada seria pior do que nao testar")
                .isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("permissao de leitura ausente vira desconhecido, nao falha do bucket")
    void deniedInspectionIsUnknown() {
        bucket.denying("corsRules");

        var cors = check(diagnostics.run(), "cors");

        assertThat(cors.status())
                .as("o bucket pode estar certo; quem nao consegue olhar e a aplicacao")
                .isEqualTo(Status.UNKNOWN);
        assertThat(cors.detail()).contains("GetBucketCors");
    }

    @Test
    @DisplayName("bucket inacessivel nao vira 'sem permissao de escrita'")
    void unreachableBucketDoesNotBlameWritePermission() {
        bucket.denying("bucketExists");

        var report = diagnostics.run();

        assertThat(check(report, "bucket_reachable").status()).isEqualTo(Status.UNKNOWN);

        // Credencial ausente faz tudo falhar, inclusive iniciar um multipart. Chamar isso
        // de "sem permissao de escrita" mandaria o operador revisar a policy errada.
        assertThat(check(report, "write_permission").status())
                .as("o diagnostico nao pode apontar uma causa que ele nao verificou")
                .isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("os JSONs prontos trazem o bucket e as origens reais")
    void snippetsCarryRealValues() {
        var report = diagnostics.run();

        var policy = report.snippets().stream()
                .filter(s -> s.id().equals("bucket_policy"))
                .findFirst().orElseThrow();

        assertThat(policy.content())
                .as("colar um exemplo generico nao configura bucket nenhum")
                .contains("arn:aws:s3:::" + report.bucket() + "/public/*")
                .doesNotContain("products/*");

        var cors = report.snippets().stream()
                .filter(s -> s.id().equals("cors"))
                .findFirst().orElseThrow();

        assertThat(cors.content()).contains("\"ExposeHeaders\": [\"ETag\"]");
    }
}
