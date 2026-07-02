package com.techora.payment.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import com.techora.payment.application.usecase.GetPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final GetPaymentUseCase getPaymentUseCase;
    private final ResponseFactory responseFactory;

    @GetMapping("/{paymentId}")
    public ResponseDto get(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable UUID paymentId) {

        return responseFactory.success(
                getPaymentUseCase.execute(paymentId, principal.getUserId()));
    }
}
