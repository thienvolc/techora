package com.techora.checkout.application;

import com.techora.checkout.controller.response.CheckoutResponse;
import com.techora.idempotency.command.IdempotencyCommand;
import com.techora.idempotency.command.IdempotencyParams;
import com.techora.idempotency.entity.IdempotencyOperation;
import org.springframework.stereotype.Component;

@Component
public class CheckoutIdempotencyFactory {

    public IdempotencyCommand<CheckoutResponse> create(CheckoutCommand command) {
        return IdempotencyCommand.<CheckoutResponse>builder()
                .userId(command.userId())
                .idempotencyKey(command.idempotencyKey())
                .operation(IdempotencyOperation.CHECKOUT)
                .params(IdempotencyParams.checkout(command.userId()))
                .responseType(CheckoutResponse.class)
                .build();
    }
}
