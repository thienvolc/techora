package com.techora.payment.controller;

import com.techora.payment.application.command.HandleVnPayIpnCommand;
import com.techora.payment.application.model.VnPayIpnReply;
import com.techora.payment.application.usecase.HandleVnPayIpnUseCase;
import com.techora.payment.controller.response.VnPayIpnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class VnPayWebhookController {
    private final HandleVnPayIpnUseCase handleVnPayIpnUseCase;

    @GetMapping("/vnpay/ipn")
    public VnPayIpnResponse handleVnPayIpn(@RequestParam Map<String, String> params) {
        VnPayIpnReply ipnReply = handleVnPayIpnUseCase.execute(new HandleVnPayIpnCommand(params));
        // INFO: VnPay IPN requires a specific response format.
        // Therefore, we return the response directly without wrapping it in a ResponseDto.
        return VnPayIpnResponse.from(ipnReply);
    }
}
