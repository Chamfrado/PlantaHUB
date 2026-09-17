package com.plantahub.api.shared.storage;

import java.util.List;

/**
 * Inspeciona o bucket <b>de fora</b>: como ele está configurado e o que um visitante
 * anônimo consegue ler dele.
 *
 * <p><b>Não existe nenhum método de escrita aqui, e isso é a razão de a interface existir
 * separada.</b> Aplicar CORS, lifecycle ou política pelo painel exigiria dar à aplicação
 * {@code s3:PutBucketCors}, {@code s3:PutLifecycleConfiguration} e
 * {@code s3:PutBucketPolicy} em produção — ou seja, a capacidade de reescrever a própria
 * segurança do bucket. O ganho seria colar um JSON uma vez; o custo seria permanente, e
 * transformaria "o acervo vendido vazou" em uma falha de aplicação em vez de uma ação
 * deliberada no console da AWS. O painel diagnostica e entrega o JSON pronto; quem aplica
 * é uma pessoa, na AWS.
 *
 * <p>A busca anônima mora aqui, e não numa porta de HTTP genérica, porque ela responde a
 * mesma pergunta que o resto: o que este bucket expõe. É o único jeito honesto de verificar
 * leitura pública — política e ACL se combinam de formas que ler o JSON da política não
 * revela.
 */
public interface BucketInspectionPort {

    record CorsRule(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            List<String> exposeHeaders
    ) {}

    record LifecycleRule(
            String id,
            boolean enabled,
            String prefix,
            Integer abortIncompleteMultipartDays
    ) {}

    /**
     * @param status código HTTP, ou {@code -1} quando a requisição nem completou
     * @param error  causa, quando {@code status} é {@code -1}
     */
    record AnonymousFetch(int status, String error) {
        public boolean readable() {
            return status >= 200 && status < 300;
        }
    }

    /** {@code true} se o bucket existe e as credenciais alcançam ele. */
    boolean bucketExists();

    /** Região real do bucket, que pode divergir da configurada. */
    String bucketRegion();

    /** Vazio quando não há CORS configurado. Lança quando falta permissão de leitura. */
    List<CorsRule> corsRules();

    /** Vazio quando não há lifecycle configurado. Lança quando falta permissão de leitura. */
    List<LifecycleRule> lifecycleRules();

    /** GET sem nenhuma credencial — exatamente o que o navegador de um visitante faz. */
    AnonymousFetch fetchWithoutCredentials(String url);

    /** Levantada quando a consulta não pôde ser feita: permissão, rede ou bucket ausente. */
    class InspectionUnavailableException extends RuntimeException {
        public InspectionUnavailableException(String message) {
            super(message);
        }
    }
}
