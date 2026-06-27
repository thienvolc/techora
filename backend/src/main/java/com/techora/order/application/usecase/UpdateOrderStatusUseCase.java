package com.techora.order.application.usecase;

import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.application.mapper.OrderMapper;
import com.techora.order.application.result.OrderResult;
import com.techora.order.application.service.OrderStatusUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {
    private final OrderMapper orderMapper;
    private final OrderStatusUpdater orderStatusUpdater;

    @Transactional
    public OrderResult execute(UpdateOrderStatusCommand command) {
        OrderStatusChange statusChange = orderStatusUpdater.update(command);
        return orderMapper.toResult(statusChange.order());
    }
}
