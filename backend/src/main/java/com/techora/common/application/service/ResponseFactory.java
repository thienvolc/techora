package com.techora.common.application.service;

import com.techora.common.application.dto.response.Meta;
import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class ResponseFactory {

    private final String appName = "techora";

    public ResponseDto success(ResponseCode responseCode) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(responseCode.getCode())
                .message(responseCode.getDefaultMessage())
                .build();
        return new ResponseDto(meta, null);
    }

    public ResponseDto success() {
        return success((Object) null);
    }

    public ResponseDto success(Object data) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(ResponseCode.SUCCESS.getCode())
                .build();
        return new ResponseDto(meta, data);
    }
}
