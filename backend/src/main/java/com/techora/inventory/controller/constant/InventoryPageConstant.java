package com.techora.inventory.controller.constant;

import org.springframework.data.domain.Sort;

public class InventoryPageConstant {

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_SIZE = 100;

    public static final Sort UPDATED_AT_DESCENDING = Sort.by(Sort.Direction.DESC, "updatedAt");
}
