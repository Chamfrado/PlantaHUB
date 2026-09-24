package com.plantahub.api.shared.exception;

/** Limite de requisicoes atingido. Vira HTTP 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("too_many_requests");
    }
}
