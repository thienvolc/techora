package com.techora.catalog.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Boolean active
) {
    public Boolean active() {
        return active == null ? Boolean.TRUE : active;
    }
}
