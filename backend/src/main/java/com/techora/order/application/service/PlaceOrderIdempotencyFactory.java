package com.techora.order.application.service;

import com.techora.order.application.model.PlaceOrderView;
import com.techora.idempotency.command.IdempotencyCommand;
import com.techora.idempotency.command.IdempotencyParams;
import com.techora.idempotency.entity.IdempotencyOperation;
import com.techora.order.application.command.PlaceOrderCommand;
import org.springframework.stereotype.Component;

@Component
public class PlaceOrderIdempotencyFactory {

    public IdempotencyCommand<PlaceOrderView> create(PlaceOrderCommand command) {
        return IdempotencyCommand.<PlaceOrderView>builder()
                .userId(command.userId())
                .idempotencyKey(command.idempotencyKey())
                .operation(IdempotencyOperation.PLACE_ORDER)
                .params(IdempotencyParams.placeOrder(command.userId()))
                .responseType(PlaceOrderView.class)
                .build();
    }
}
