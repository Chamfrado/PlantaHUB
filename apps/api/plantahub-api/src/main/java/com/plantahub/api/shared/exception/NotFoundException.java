package com.plantahub.api.shared.exception;

/** Recurso inexistente. Mapeado para 404 na superficie administrativa. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
