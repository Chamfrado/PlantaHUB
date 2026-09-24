package com.plantahub.api.shared.exception;

import com.plantahub.api.web.dto.profile.ProfileIncompleteResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Mantido como 400 de proposito.
     *
     * <p>A superficie publica antiga usa {@code IllegalArgumentException} para tudo, e o app
     * web le a mensagem do corpo. Reescrever esse contrato inteiro seria uma mudanca de
     * comportamento escondida dentro de um trabalho de catalogo; as excecoes tipadas abaixo
     * sao o caminho novo, adotado caso a caso.
     *
     * <p>A leitura publica de produto ja migrou para {@link NotFoundException}: um E2E
     * mostrou que ela era o unico lugar onde o status realmente importava — a pagina de
     * detalhe so mostra "Produto nao encontrado" em 404 e, com 400, uma URL de produto
     * despublicado caia numa tela de falha com botao de repetir que nunca ia funcionar.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<ProfileIncompleteResponse> handleProfileIncomplete(ProfileIncompleteException ex) {
        return ResponseEntity.badRequest().body(
                new ProfileIncompleteResponse("profile_incomplete", ex.getMissingFields())
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());

        if (!ex.getReasons().isEmpty()) {
            // Todos os motivos de uma vez: descobrir um por tentativa seria hostil.
            body.put("reasons", ex.getReasons());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Erros de validacao de corpo nao tinham tratamento nenhum: o cliente recebia o HTML
     * de erro padrao do container em vez de saber qual campo estava errado.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "invalido" : error.getDefaultMessage()
                ))
                .toList();

        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed",
                "fields", fields
        ));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
    }

    /** Sem isto, um 403 do Spring Security chega ao cliente como pagina de erro do container. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
    }

    /** Constraint do banco violada (slug repetido, chave duplicada) e conflito, nao erro do servidor. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "data_integrity_violation"));
    }
}
