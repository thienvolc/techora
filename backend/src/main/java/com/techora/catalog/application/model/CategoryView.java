package com.techora.catalog.application.model;

import java.time.Instant;
import java.util.UUID;

public record CategoryView(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
