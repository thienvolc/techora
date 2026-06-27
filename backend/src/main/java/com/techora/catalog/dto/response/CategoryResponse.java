package com.techora.catalog.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
