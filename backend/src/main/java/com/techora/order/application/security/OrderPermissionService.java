package com.techora.order.application.security;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.domain.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPermissionService {
    private final OrderRepository orderRepository;

    public Order getOwnedOrderForUpdate(UUID userId, UUID orderId) {
        return orderRepository.findLockedWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }
}
