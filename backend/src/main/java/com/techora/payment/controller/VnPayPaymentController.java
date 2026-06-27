package com.techora.payment.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.application.usecase.InitiateVnPayPaymentUseCase;
import com.techora.payment.controller.request.CreatePaymentRequest;
import com.techora.payment.controller.response.VnPayInitiatePaymentResponse;
import com.techora.payment.controller.service.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class VnPayPaymentController {
    private final InitiateVnPayPaymentUseCase initiateVnPayPaymentUseCase;
    private final ClientIpResolver clientIpResolver;
    private final ResponseFactory responseFactory;

    @PostMapping("/vnpay")
    public ResponseDto initVnPayPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        var command = InitiateVnPayPaymentCommand.builder()
                .userId(principal.getUserId())
                .orderId(request.orderId())
                .ipAddress(clientIpResolver.resolve(servletRequest))
                .idempotencyKey(idempotencyKey)
                .build();

        var result = initiateVnPayPaymentUseCase.execute(command);
        return responseFactory.success(VnPayInitiatePaymentResponse.from(result));
    }
}
