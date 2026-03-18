package com.plantahub.api.shared.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ProfileIncompleteException extends RuntimeException {

    private final List<String> missingFields;

    public ProfileIncompleteException(List<String> missingFields) {
        super("profile_incomplete");
        this.missingFields = missingFields;
    }

}
