package com.techora.catalog.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.common.application.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class ProductPurchasePolicy {

    public void validatePurchasable(CatalogProductSnapshot product) {
        validateAvailable(product);
    }

    public void validateAvailable(CatalogProductSnapshot product) {
        if (product.isInactive()) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
    }
}
