package com.techora.catalog.projection.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryProjectionSnapshot(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
