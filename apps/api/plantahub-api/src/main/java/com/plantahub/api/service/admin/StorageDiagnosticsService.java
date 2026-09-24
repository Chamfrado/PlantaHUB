package com.plantahub.api.service.admin;

import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.ProductMediaRepository;
import com.plantahub.api.shared.storage.BucketInspectionPort;
import com.plantahub.api.shared.storage.BucketInspectionPort.InspectionUnavailableException;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.shared.storage.PublicMediaUrls;
import com.plantahub.api.shared.storage.StorageKeyFactory;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.Check;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.DiagnosticsReport;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.Snippet;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Responde, com evidência, a pergunta que hoje só aparece como sintoma: <b>o bucket está
 * configurado do jeito que esta aplicação precisa?</b>
 *
 * <p>Cada item aqui existe porque a falha correspondente é silenciosa. Sem
 * {@code ExposeHeaders: ETag} no CORS, um upload multipart de 400 MB sobe inteiro e não
 * fecha, sem mensagem de erro útil. Sem a regra de lifecycle, partes abandonadas são
 * cobradas para sempre e não aparecem em listagem nenhuma. E se a política de leitura
 * pública alcançar {@code products/}, o acervo vendido fica aberto — sem que nada no
 * sistema se comporte de forma diferente.
 *
 * <p><b>Só lê.</b> Ver {@link BucketInspectionPort} para por que aplicar a configuração
 * pelo painel seria uma troca ruim.
 */
@Service
public class StorageDiagnosticsService {

    private static final Logger log = LoggerFactory.getLogger(StorageDiagnosticsService.class);

    /** Métodos que o navegador usa ao enviar direto para o bucket. */
    private static final List<String> REQUIRED_CORS_METHODS = List.of("PUT", "POST", "GET", "HEAD");

    private final BucketInspectionPort inspection;
    private final ObjectStoragePort storage;
    private final PublicMediaUrls publicUrls;
    private final ProductMediaRepository mediaRepo;
    private final DigitalAssetRepository assetRepo;

    private final String bucket;
    private final String configuredRegion;
    private final List<String> webOrigins;

    public StorageDiagnosticsService(
            BucketInspectionPort inspection,
            ObjectStoragePort storage,
            PublicMediaUrls publicUrls,
            ProductMediaRepository mediaRepo,
            DigitalAssetRepository assetRepo,
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.s3.region}") String configuredRegion,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins
    ) {
        this.inspection = inspection;
        this.storage = storage;
        this.publicUrls = publicUrls;
        this.mediaRepo = mediaRepo;
        this.assetRepo = assetRepo;
        this.bucket = bucket;
        this.configuredRegion = configuredRegion;
        this.webOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Transactional(readOnly = true)
    public DiagnosticsReport run() {
        List<Check> checks = new ArrayList<>();

        Check reachable = checkBucketReachable();

        checks.add(reachable);
        checks.add(checkRegion());
        checks.addAll(checkCors());
        checks.add(checkLifecycle());
        checks.add(checkWritePermission(reachable.status() == Status.OK));
        checks.add(checkPublicPrefixReadable());
        checks.add(checkPaidPrefixPrivate());

        return new DiagnosticsReport(
                bucket, configuredRegion, publicUrls.base(), Instant.now(), checks, snippets());
    }

    // ------------------------------------------------------------------
    // Verificações
    // ------------------------------------------------------------------

    private Check checkBucketReachable() {
        try {
            return inspection.bucketExists()
                    ? ok("bucket_reachable", "Bucket acessível",
                        "As credenciais alcançam o bucket " + bucket + ".")
                    : fail("bucket_reachable", "Bucket acessível",
                        "O bucket " + bucket + " não existe, ou as credenciais não enxergam ele.",
                        "Nada relacionado a arquivo funciona: nem upload, nem download, nem imagem.");
        } catch (InspectionUnavailableException e) {
            return unknown("bucket_reachable", "Bucket acessível", e.getMessage(),
                    "Sem isto, nenhuma das outras verificações é confiável.");
        }
    }

    private Check checkRegion() {
        try {
            String real = inspection.bucketRegion();

            return real.equalsIgnoreCase(configuredRegion)
                    ? ok("region_matches", "Região confere",
                        "Configurada e real são " + real + ".")
                    : fail("region_matches", "Região confere",
                        "A aplicação está configurada para " + configuredRegion
                                + ", mas o bucket está em " + real + ".",
                        "URLs assinadas nascem com a região errada e a S3 as recusa com "
                                + "SignatureDoesNotMatch — upload e download param.");
        } catch (InspectionUnavailableException e) {
            return unknown("region_matches", "Região confere", e.getMessage(),
                    "Uma divergência de região quebra toda URL assinada.");
        }
    }

    private List<Check> checkCors() {
        List<BucketInspectionPort.CorsRule> rules;

        try {
            rules = inspection.corsRules();
        } catch (InspectionUnavailableException e) {
            return List.of(unknown("cors", "CORS do bucket",
                    e.getMessage() + " (falta s3:GetBucketCors?)",
                    "Sem CORS correto, o navegador não consegue enviar arquivo nenhum."));
        }

        if (rules.isEmpty()) {
            return List.of(fail("cors", "CORS do bucket",
                    "O bucket não tem nenhuma regra de CORS.",
                    "O navegador bloqueia o envio direto para a S3 antes mesmo de sair a "
                            + "primeira requisição."));
        }

        List<Check> checks = new ArrayList<>();

        // Este é o item que mais custa caro quando falta, e o mais fácil de esquecer: sem
        // expor o ETag, o navegador não lê o ETag de cada parte e o multipart nunca fecha.
        boolean exposesEtag = rules.stream()
                .flatMap(rule -> rule.exposeHeaders().stream())
                .anyMatch(header -> "etag".equalsIgnoreCase(header));

        checks.add(exposesEtag
                ? ok("cors_expose_etag", "CORS expõe o cabeçalho ETag",
                    "ExposeHeaders inclui ETag.")
                : fail("cors_expose_etag", "CORS expõe o cabeçalho ETag",
                    "Nenhuma regra de CORS lista ETag em ExposeHeaders.",
                    "Arquivos acima de 100 MB sobem inteiros e nunca terminam: o navegador "
                            + "não consegue ler o ETag das partes para fechar o multipart. "
                            + "É a causa número um de \"upload falha em silêncio\"."));

        List<String> missingMethods = REQUIRED_CORS_METHODS.stream()
                .filter(method -> rules.stream().noneMatch(rule ->
                        rule.allowedMethods().stream().anyMatch(method::equalsIgnoreCase)))
                .toList();

        checks.add(missingMethods.isEmpty()
                ? ok("cors_methods", "CORS permite os métodos necessários",
                    "PUT, POST, GET e HEAD liberados.")
                : fail("cors_methods", "CORS permite os métodos necessários",
                    "Faltam: " + String.join(", ", missingMethods) + ".",
                    "O navegador recusa a requisição antes de enviá-la."));

        List<String> uncoveredOrigins = webOrigins.stream()
                .filter(origin -> rules.stream().noneMatch(rule ->
                        rule.allowedOrigins().contains("*") || rule.allowedOrigins().contains(origin)))
                .toList();

        checks.add(uncoveredOrigins.isEmpty()
                ? ok("cors_origins", "CORS cobre as origens do site",
                    webOrigins.isEmpty()
                            ? "Nenhuma origem configurada em app.cors.allowed-origins."
                            : String.join(", ", webOrigins) + " permitidas.")
                : fail("cors_origins", "CORS cobre as origens do site",
                    "Sem regra para: " + String.join(", ", uncoveredOrigins) + ".",
                    "O painel carrega, mas todo upload feito a partir dessas origens é "
                            + "bloqueado pelo navegador."));

        return checks;
    }

    private Check checkLifecycle() {
        try {
            var rules = inspection.lifecycleRules();

            var abortRule = rules.stream()
                    .filter(rule -> rule.enabled() && rule.abortIncompleteMultipartDays() != null)
                    .findFirst();

            return abortRule
                    .map(rule -> ok("lifecycle_abort_multipart",
                            "Lifecycle aborta multipart incompleto",
                            "Regra \"" + rule.id() + "\" aborta após "
                                    + rule.abortIncompleteMultipartDays() + " dia(s)."))
                    .orElseGet(() -> warn("lifecycle_abort_multipart",
                            "Lifecycle aborta multipart incompleto",
                            "Nenhuma regra ativa com AbortIncompleteMultipartUpload.",
                            "Partes de uploads abandonados continuam sendo cobradas para "
                                    + "sempre e não aparecem em nenhuma listagem do bucket. "
                                    + "A aplicação aborta o que ela mesma inicia, mas não "
                                    + "alcança o que morreu com a aba do navegador."));
        } catch (InspectionUnavailableException e) {
            return unknown("lifecycle_abort_multipart", "Lifecycle aborta multipart incompleto",
                    e.getMessage() + " (falta s3:GetLifecycleConfiguration?)",
                    "Sem a regra, partes órfãs são cobradas indefinidamente.");
        }
    }

    /**
     * Prova de escrita sem escrever nada.
     *
     * <p>Iniciar e abortar um multipart exercita exatamente as permissões que o upload
     * precisa ({@code s3:PutObject}, {@code s3:AbortMultipartUpload}) e não deixa objeto
     * algum para trás — nem quando falha no meio, porque um multipart iniciado e não
     * concluído não cria objeto.
     */
    private Check checkWritePermission(boolean bucketReachable) {
        if (!bucketReachable) {
            // Sem alcancar o bucket, um erro aqui nao diz nada sobre permissao de escrita:
            // a causa e a mesma de antes. Reportar FAIL mandaria o operador revisar a
            // policy de escrita quando o problema real e credencial ou nome de bucket.
            return unknown("write_permission", "Permissão de escrita",
                    "Não verificado: o bucket não respondeu.",
                    "Resolva o item anterior primeiro.");
        }

        String probeKey = StorageKeyFactory.PUBLIC_PREFIX + ".diagnostico/" + UUID.randomUUID();

        try {
            String uploadId = storage.initiateMultipart(probeKey, "application/octet-stream");

            try {
                storage.abortMultipart(probeKey, uploadId);
            } catch (RuntimeException e) {
                log.warn("Diagnóstico: multipart de teste iniciado mas não abortado em {}", probeKey, e);

                return warn("write_permission", "Permissão de escrita",
                        "Iniciar funcionou, abortar falhou: " + e.getMessage(),
                        "Faltando s3:AbortMultipartUpload, uploads cancelados deixam partes "
                                + "cobradas para trás.");
            }

            return ok("write_permission", "Permissão de escrita",
                    "Multipart de teste iniciado e abortado com sucesso.");
        } catch (RuntimeException e) {
            return fail("write_permission", "Permissão de escrita",
                    "Não foi possível iniciar um multipart: " + e.getMessage(),
                    "Nenhum upload pelo painel vai funcionar. Confira s3:PutObject e "
                            + "s3:ListBucketMultipartUploads na policy da aplicação.");
        }
    }

    private Check checkPublicPrefixReadable() {
        var sample = mediaRepo.findAnyPublicSample();

        if (sample.isEmpty()) {
            return unknown("public_prefix_readable", "Imagens são legíveis sem login",
                    "Nenhuma imagem enviada pelo painel ainda — não há o que testar.",
                    "Envie uma imagem na aba Imagens de qualquer produto e rode de novo.");
        }

        String url = sample.get().getPublicUrl();
        var fetch = inspection.fetchWithoutCredentials(url);

        if (fetch.status() == -1) {
            return unknown("public_prefix_readable", "Imagens são legíveis sem login",
                    "A requisição não completou: " + fetch.error(),
                    "Pode ser rede da própria aplicação, e não configuração do bucket.");
        }

        return fetch.readable()
                ? ok("public_prefix_readable", "Imagens são legíveis sem login",
                    "Um visitante anônimo recebe HTTP " + fetch.status() + " em " + url + ".")
                : fail("public_prefix_readable", "Imagens são legíveis sem login",
                    "Um visitante anônimo recebe HTTP " + fetch.status() + " em " + url + ".",
                    "Capa e galeria aparecem quebradas para quem ainda não comprou — ou "
                            + "seja, para todo visitante novo. Aplique a política de leitura "
                            + "do prefixo public/.");
    }

    /**
     * A verificação mais importante da tela, e a única cuja falha é um vazamento.
     *
     * <p>Usa sempre o endpoint direto do bucket, nunca a base pública configurada: um 403
     * no CloudFront não prova nada sobre o que a S3 entrega a quem pede direto a ela.
     */
    private Check checkPaidPrefixPrivate() {
        var sample = assetRepo.findAnyPaidSample();

        if (sample.isEmpty()) {
            return unknown("paid_prefix_private", "Arquivos vendidos NÃO são públicos",
                    "Nenhum arquivo registrado ainda — não há o que testar.",
                    "Rode de novo depois da reconciliação ou do primeiro upload de arquivo.");
        }

        String url = publicUrls.directBucketUrlFor(sample.get().getStorageKey());
        var fetch = inspection.fetchWithoutCredentials(url);

        if (fetch.status() == -1) {
            return unknown("paid_prefix_private", "Arquivos vendidos NÃO são públicos",
                    "A requisição não completou: " + fetch.error(),
                    "Não dá para concluir nada; tente de novo.");
        }

        return fetch.readable()
                ? fail("paid_prefix_private", "Arquivos vendidos NÃO são públicos",
                    "Qualquer pessoa com a URL baixa " + sample.get().getStorageKey()
                            + " (HTTP " + fetch.status() + ").",
                    "O acervo pago está aberto: a regra de entitlement do sistema vira "
                            + "decoração. Restrinja a política de leitura ao prefixo public/ "
                            + "e a mais nada.")
                : ok("paid_prefix_private", "Arquivos vendidos NÃO são públicos",
                    "Sem credencial, a S3 responde HTTP " + fetch.status()
                            + " para um arquivo pago.");
    }

    // ------------------------------------------------------------------
    // JSONs prontos
    // ------------------------------------------------------------------

    private List<Snippet> snippets() {
        return List.of(
                new Snippet("cors", "CORS do bucket",
                        "Console da AWS → S3 → " + bucket + " → Permissions → "
                                + "Cross-origin resource sharing (CORS).",
                        corsJson()),
                new Snippet("lifecycle", "Regra de lifecycle",
                        "S3 → " + bucket + " → Management → Lifecycle rules → Create rule, "
                                + "marcando apenas \"Delete expired object delete markers or "
                                + "incomplete multipart uploads\".",
                        lifecycleJson()),
                new Snippet("bucket_policy", "Política de leitura pública",
                        "S3 → " + bucket + " → Permissions → Bucket policy. Libera somente "
                                + "public/*: arquivos vendidos ficam de fora por construção.",
                        bucketPolicyJson()),
                new Snippet("iam", "Permissões da aplicação",
                        "IAM → a role ou o usuário que a aplicação usa. Inclui as leituras "
                                + "de configuração que esta tela precisa.",
                        iamPolicyJson()));
    }

    private String corsJson() {
        String origins = webOrigins.isEmpty()
                ? "\"https://seu-dominio.com.br\""
                : webOrigins.stream().map(StorageDiagnosticsService::quote)
                        .reduce((a, b) -> a + ", " + b).orElse("");

        return """
        [
          {
            "AllowedOrigins": [%s],
            "AllowedMethods": ["PUT", "POST", "GET", "HEAD"],
            "AllowedHeaders": ["*"],
            "ExposeHeaders": ["ETag"],
            "MaxAgeSeconds": 3000
          }
        ]""".formatted(origins);
    }

    private String lifecycleJson() {
        return """
        {
          "Rules": [
            {
              "ID": "abortar-multipart-incompleto",
              "Status": "Enabled",
              "Filter": { "Prefix": "" },
              "AbortIncompleteMultipartUpload": { "DaysAfterInitiation": 7 }
            }
          ]
        }""";
    }

    private String bucketPolicyJson() {
        return """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "LeituraPublicaSomenteDeMidia",
              "Effect": "Allow",
              "Principal": "*",
              "Action": "s3:GetObject",
              "Resource": "arn:aws:s3:::%s/public/*"
            }
          ]
        }""".formatted(bucket);
    }

    private String iamPolicyJson() {
        return """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "Objetos",
              "Effect": "Allow",
              "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:AbortMultipartUpload",
                "s3:ListMultipartUploadParts"
              ],
              "Resource": "arn:aws:s3:::%s/*"
            },
            {
              "Sid": "BucketELeituraDeConfiguracao",
              "Effect": "Allow",
              "Action": [
                "s3:ListBucket",
                "s3:ListBucketMultipartUploads",
                "s3:GetBucketLocation",
                "s3:GetBucketCORS",
                "s3:GetLifecycleConfiguration"
              ],
              "Resource": "arn:aws:s3:::%s"
            }
          ]
        }""".formatted(bucket, bucket);
    }

    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    // ------------------------------------------------------------------

    private static Check ok(String id, String title, String detail) {
        return new Check(id, title, Status.OK, detail, null);
    }

    private static Check warn(String id, String title, String detail, String impact) {
        return new Check(id, title, Status.WARN, detail, impact);
    }

    private static Check fail(String id, String title, String detail, String impact) {
        return new Check(id, title, Status.FAIL, detail, impact);
    }

    private static Check unknown(String id, String title, String detail, String impact) {
        return new Check(id, title, Status.UNKNOWN, detail, impact);
    }
}
