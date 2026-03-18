package com.plantahub.api.web.dto.profile;

import java.util.List;

public record ProfileIncompleteResponse(
        String error,
        List<String> missingFields
) { }
