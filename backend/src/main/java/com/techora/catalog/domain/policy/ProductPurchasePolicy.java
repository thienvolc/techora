package com.techora.catalog.domain.policy;

import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.application.model.CatalogProductSnapshot;
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
