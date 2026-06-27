package com.techora.catalog.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.dto.ProductSnapshot;
import com.techora.common.application.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class ProductPurchasePolicy {

    public void validatePurchasable(ProductSnapshot product) {
        validateAvailable(product);
    }

    public void validateAvailable(ProductSnapshot product) {
        if (product.isInactive()) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
    }
}
