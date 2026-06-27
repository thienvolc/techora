package com.techora.order.controller.request;

import com.techora.order.domain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}
