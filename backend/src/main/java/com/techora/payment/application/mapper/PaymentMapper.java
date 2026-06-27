package com.techora.payment.application.mapper;

import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResult toResult(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getProviderReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
