package com.techora.order.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.usecase.OrderQueryService;
import com.techora.order.application.usecase.UpdateOrderStatusUseCase;
import com.techora.order.controller.request.UpdateOrderStatusRequest;
import com.techora.order.controller.response.OrderResponse;
import com.techora.orderhistory.OrderHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderQueryService orderQueryService;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderHistoryService orderHistoryService;
    private final ResponseFactory responseFactory;

    @GetMapping("/{orderId}")
    public ResponseDto getOrder(@PathVariable UUID orderId) {
        return responseFactory.success(
                OrderResponse.from(orderQueryService.getAdminOrder(orderId)));
    }

    @GetMapping("/status-summary")
    public ResponseDto getStatusSummary() {
        return responseFactory.success(
                orderQueryService.countOrdersByStatus());
    }

    @GetMapping("/{orderId}/events")
    public ResponseDto getOrderEvents(@PathVariable UUID orderId) {
        return responseFactory.success(
                orderHistoryService.getAdminEvents(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseDto updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        var order = updateOrderStatusUseCase.execute(
                UpdateOrderStatusCommand.fromAdmin(
                        orderId,
                        request.status(),
                        principal.getUserId(),
                        principal.getUsername()
                ));
        return responseFactory.success(
                OrderResponse.from(order));
    }
}
