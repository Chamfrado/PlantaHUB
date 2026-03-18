package com.plantahub.api.web.dto.profile;

import java.util.List;

public record ProfileStatusResponse(
        boolean profileCompleted,
        boolean cpfLocked,
        List<String> missingFields
) {}