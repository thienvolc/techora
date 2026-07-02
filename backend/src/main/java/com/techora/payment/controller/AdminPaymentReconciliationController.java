package com.techora.payment.controller;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.payment.application.service.PaymentReconciliationService;
import com.techora.payment.application.model.PaymentReconciliationDetails;
import com.techora.payment.controller.constant.PaymentPageConstant;
import com.techora.payment.controller.request.ResolvePaymentReconciliationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payments/reconciliations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentReconciliationController {
    private final PaymentReconciliationService paymentReconciliationService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getUnresolved(
            @RequestParam(defaultValue = PaymentPageConstant.DEFAULT_PAGE) int page,
            @Max(PaymentPageConstant.MAX_SIZE)
            @RequestParam(defaultValue = PaymentPageConstant.DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size, PaymentPageConstant.CREATED_AT_DESCENDING);
        PageResponse<PaymentReconciliationDetails> payments =
                paymentReconciliationService.getUnresolved(pageable);
        return responseFactory.success(payments);
    }

    @GetMapping("/{attemptId}")
    public ResponseDto get(@PathVariable UUID attemptId) {
        return responseFactory.success(paymentReconciliationService.get(attemptId));
    }

    @PostMapping("/{attemptId}/resolve")
    public ResponseDto resolve(@PathVariable UUID attemptId,
                               @Valid @RequestBody ResolvePaymentReconciliationRequest request) {

        return responseFactory.success(
                paymentReconciliationService.resolve(attemptId, request.note()));
    }
}
