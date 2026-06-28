package com.techora.payment.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolvePaymentReconciliationRequest(
        @NotBlank
        @Size(max = 1000)
        String note
) {
}
