package com.techora.payment.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.payment.application.usecase.GetVnPayReturnUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class VnPayReturnController {
    private final GetVnPayReturnUseCase getVnPayReturnUseCase;
    private final ResponseFactory responseFactory;

    @GetMapping("/vnpay/return/{txnRef}")
    public ResponseDto handleReturn(@PathVariable String txnRef) {
        return responseFactory.success(getVnPayReturnUseCase.execute(txnRef));
    }
}
