package com.techora.payment.application.mapper;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.payment.application.model.PaymentReconciliationDetails;
import com.techora.payment.domain.entity.PaymentAttempt;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationMapper {

    public PageResponse<PaymentReconciliationDetails> toPageResponse(Page<PaymentAttempt> attempts) {
        return new PageResponse<>(
                attempts.getContent().stream()
                        .map(PaymentReconciliationDetails::from)
                        .toList(),
                attempts.getNumber(),
                attempts.getSize(),
                attempts.getTotalElements(),
                attempts.getTotalPages()
        );
    }
}
