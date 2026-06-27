package com.techora.payment.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techora.payment.application.result.VnPayIpnResult;

public record VnPayIpnResponse(
        @JsonProperty("RspCode")
        String responseCode,

        @JsonProperty("Message")
        String message
) {

    public static VnPayIpnResponse from(VnPayIpnResult result) {
        return new VnPayIpnResponse(result.responseCode(), result.message());
    }
}
