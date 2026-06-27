package com.techora.order.controller.constant;

import org.springframework.data.domain.Sort;

public class OrderPageConstant {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_SIZE = 100;
    public static final Sort CREATED_AT_DESCENDING = Sort.by(Sort.Direction.DESC, "createdAt");
}
