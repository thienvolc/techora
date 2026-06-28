package com.techora.checkout.controller;

import com.techora.checkout.application.CheckoutCommand;
import com.techora.checkout.application.CheckoutUseCase;
import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.common.infra.service.UserPrincipal;
import com.techora.common.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/checkout")
    public ResponseDto checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return responseFactory.success(
                checkoutUseCase.execute(new CheckoutCommand(
                        principal.getUserId(),
                        clientIpResolver.resolve(servletRequest),
                        idempotencyKey
                )));
    }
}
