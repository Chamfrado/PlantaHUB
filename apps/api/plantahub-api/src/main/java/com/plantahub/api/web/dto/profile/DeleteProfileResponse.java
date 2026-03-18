package com.plantahub.api.web.dto.profile;

import java.time.Instant;

public record DeleteProfileResponse(
        boolean deleted,
        Instant deletedAt
) {}