package com.plantahub.api.support;

import com.plantahub.api.shared.storage.BucketInspectionPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bucket configurável em memória, para o diagnóstico ser testado sem AWS.
 *
 * <p>Começa no estado <b>correto</b>: cada teste estraga só o que quer observar, e a
 * asserção fica sobre a diferença em vez de sobre uma montagem inteira.
 */
public class FakeBucketInspection implements BucketInspectionPort {

    private boolean exists = true;
    private String region = "us-east-2";

    private List<CorsRule> cors = new ArrayList<>(List.of(new CorsRule(
            List.of("http://localhost:5173"),
            List.of("PUT", "POST", "GET", "HEAD"),
            List.of("*"),
            List.of("ETag"))));

    private List<LifecycleRule> lifecycle = new ArrayList<>(List.of(
            new LifecycleRule("abortar-multipart-incompleto", true, "", 7)));

    private final Map<String, Integer> anonymousStatuses = new HashMap<>();
    private int defaultAnonymousStatus = 403;

    private String failing;

    // ---------- controles de teste ----------

    /**
     * Volta ao estado saudavel.
     *
     * <p>O bean e singleton no contexto do Spring, e {@code @Transactional} so desfaz o
     * banco: sem chamar isto no setUp, um bucket estragado por um teste continua estragado
     * no proximo, e a suite passa a depender da ordem de execucao.
     */
    public void reset() {
        exists = true;
        region = "us-east-2";
        cors = new ArrayList<>(List.of(new CorsRule(
                List.of("http://localhost:5173"),
                List.of("PUT", "POST", "GET", "HEAD"),
                List.of("*"),
                List.of("ETag"))));
        lifecycle = new ArrayList<>(List.of(
                new LifecycleRule("abortar-multipart-incompleto", true, "", 7)));
        anonymousStatuses.clear();
        defaultAnonymousStatus = 403;
        failing = null;
    }


    public FakeBucketInspection withoutCors() {
        cors = new ArrayList<>();
        return this;
    }

    public FakeBucketInspection withCors(CorsRule... rules) {
        cors = new ArrayList<>(List.of(rules));
        return this;
    }

    public FakeBucketInspection withoutLifecycle() {
        lifecycle = new ArrayList<>();
        return this;
    }

    public FakeBucketInspection inRegion(String value) {
        region = value;
        return this;
    }

    public FakeBucketInspection missing() {
        exists = false;
        return this;
    }

    /** Faz a operação informada lançar, como faria uma permissão de leitura ausente. */
    public FakeBucketInspection denying(String operation) {
        failing = operation;
        return this;
    }

    /** Define o que um visitante anônimo recebe numa URL específica. */
    public FakeBucketInspection anonymousSees(String url, int status) {
        anonymousStatuses.put(url, status);
        return this;
    }

    public FakeBucketInspection anonymousSeesEverything(int status) {
        defaultAnonymousStatus = status;
        return this;
    }

    // ---------- porta ----------

    @Override
    public boolean bucketExists() {
        guard("bucketExists");
        return exists;
    }

    @Override
    public String bucketRegion() {
        guard("bucketRegion");
        return region;
    }

    @Override
    public List<CorsRule> corsRules() {
        guard("corsRules");
        return cors;
    }

    @Override
    public List<LifecycleRule> lifecycleRules() {
        guard("lifecycleRules");
        return lifecycle;
    }

    @Override
    public AnonymousFetch fetchWithoutCredentials(String url) {
        return new AnonymousFetch(
                anonymousStatuses.getOrDefault(url, defaultAnonymousStatus), null);
    }

    private void guard(String operation) {
        if (operation.equals(failing)) {
            throw new InspectionUnavailableException(operation + " falhou: AccessDenied");
        }
    }
}
