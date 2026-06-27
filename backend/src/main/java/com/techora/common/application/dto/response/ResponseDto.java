package com.techora.common.application.dto.response;

import jakarta.annotation.Nullable;

public record ResponseDto(
        Meta meta,
        @Nullable Object data
) {
}
