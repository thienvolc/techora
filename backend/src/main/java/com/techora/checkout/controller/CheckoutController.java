package com.techora.checkout.controller;

import com.techora.checkout.application.CheckoutCommand;
import com.techora.checkout.application.CheckoutUseCase;
import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutUseCase checkoutUseCase;
    private final ResponseFactory responseFactory;

    @PostMapping("/checkout")
    public ResponseDto checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return responseFactory.success(
                checkoutUseCase.execute(new CheckoutCommand(principal.getUserId(), idempotencyKey)));
    }
}
