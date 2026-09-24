package com.plantahub.api.shared.exception;

import java.util.List;

/**
 * A operacao e valida, mas o estado atual a impede.
 *
 * <p>Carrega uma lista de motivos porque a validacao de publicacao precisa dizer
 * <b>todos</b> os itens que faltam de uma vez: mandar o admin descobrir um por vez,
 * tentativa apos tentativa, e hostil.
 */
public class ConflictException extends RuntimeException {

    private final List<String> reasons;

    public ConflictException(String message) {
        this(message, List.of());
    }

    public ConflictException(String message, List<String> reasons) {
        super(message);
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public List<String> getReasons() {
        return reasons;
    }
}
