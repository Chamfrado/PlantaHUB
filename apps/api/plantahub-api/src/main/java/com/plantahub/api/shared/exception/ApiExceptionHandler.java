package com.plantahub.api.shared.exception;

import com.plantahub.api.web.dto.profile.ProfileIncompleteResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

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
}
