package com.techora.orderhistory;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderHistoryController {
    private final OrderHistoryService orderHistoryService;
    private final ResponseFactory responseFactory;

    @GetMapping("/{orderId}/events")
    public ResponseDto getOrderEvents(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable UUID orderId) {

        return responseFactory.success(
                orderHistoryService.getUserEvents(principal.getUserId(), orderId));
    }
}
