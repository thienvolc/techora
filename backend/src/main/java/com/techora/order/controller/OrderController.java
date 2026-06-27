package com.techora.order.controller;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import com.techora.order.application.result.OrderResult;
import com.techora.order.application.usecase.OrderQueryService;
import com.techora.order.controller.constant.OrderPageConstant;
import com.techora.order.controller.response.OrderResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderQueryService orderQueryService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @Min(0)
            @RequestParam(defaultValue = OrderPageConstant.DEFAULT_PAGE) int page,
            @Min(1)
            @Max(OrderPageConstant.MAX_SIZE)
            @RequestParam(defaultValue = OrderPageConstant.DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, OrderPageConstant.CREATED_AT_DESCENDING);
        PageResponse<OrderResult> orderPage =
                orderQueryService.getUserOrders(principal.getUserId(), pageable);
        return responseFactory.success(OrderResponse.from(orderPage));
    }

    @GetMapping("/{orderId}")
    public ResponseDto getOrder(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable UUID orderId) {

        OrderResult order = orderQueryService.getUserOrder(principal.getUserId(), orderId);
        return responseFactory.success(OrderResponse.from(order));
    }
}
