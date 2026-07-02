package com.techora.orderhistory;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.orderhistory.application.view.AdminOrderHistoryView;
import com.techora.orderhistory.application.view.OrderHistoryView;
import com.techora.orderhistory.dto.OrderHistoryRecord;
import com.techora.orderhistory.entity.OrderHistoryEntity;
import com.techora.orderhistory.mapper.OrderHistoryMapper;
import com.techora.orderhistory.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderHistoryRepository historyRepository;
    private final OrderHistoryMapper historyMapper;

    public void record(OrderHistoryRecord record) {
        historyRepository.save(
                historyMapper.toEntity(record));
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryView> getUserEvents(UUID userId, UUID orderId) {
        List<OrderHistoryEntity> histories =
                historyRepository.findByOrderIdAndOwnerUserIdOrderByCreatedAtAsc(orderId, userId);
        if (histories.isEmpty()) {
            throw new BusinessException(ResponseCode.ORDER_NOT_FOUND);
        }
        return histories.stream()
                .map(historyMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOrderHistoryView> getAdminEvents(UUID orderId) {
        return historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(historyMapper::toAdminView)
                .toList();
    }
}
