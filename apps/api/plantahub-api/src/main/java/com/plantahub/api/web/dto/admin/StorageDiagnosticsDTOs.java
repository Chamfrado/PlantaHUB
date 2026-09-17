package com.plantahub.api.web.dto.admin;

import java.time.Instant;
import java.util.List;

public final class StorageDiagnosticsDTOs {

    private StorageDiagnosticsDTOs() {}

    public enum Status {
        /** Conferido e correto. */
        OK,
        /** Funciona, mas vai doer: cobrança silenciosa, lentidão, limite apertado. */
        WARN,
        /** Quebrado, ou inseguro. */
        FAIL,
        /** Não deu para verificar — falta permissão de leitura ou falta dado de amostra. */
        UNKNOWN
    }

    /**
     * @param impact o que acontece na prática se não for resolvido. Sem isto, uma lista de
     *               status vira ruído que o operador aprende a ignorar.
     */
    public record Check(
            String id,
            String title,
            Status status,
            String detail,
            String impact
    ) {}

    /** Um JSON pronto para colar no console da AWS. */
    public record Snippet(
            String id,
            String title,
            String description,
            String content
    ) {}

    public record DiagnosticsReport(
            String bucket,
            String configuredRegion,
            String publicBaseUrl,
            Instant ranAt,
            List<Check> checks,
            List<Snippet> snippets
    ) {}
}
