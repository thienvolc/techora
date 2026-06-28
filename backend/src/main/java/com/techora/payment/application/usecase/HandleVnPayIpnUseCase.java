package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.payment.application.command.ApplyProviderPaymentResultCommand;
import com.techora.payment.application.command.HandleVnPayIpnCommand;
import com.techora.payment.application.exception.InvalidVnPayPayloadException;
import com.techora.payment.application.exception.InvalidVnPaySignatureException;
import com.techora.payment.application.policy.VnPayIpnResponsePolicy;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.port.gateway.VnPayPaymentResult;
import com.techora.payment.application.result.VnPayIpnResult;
import com.techora.payment.application.service.PaymentProvider;
import com.techora.payment.application.service.ProviderPaymentResultApplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HandleVnPayIpnUseCase {
    private static final PaymentProvider PROVIDER_NAME = PaymentProvider.VNPAY;

    private final VnPayGatewayPort vnPayGatewayPort;
    private final VnPayIpnResponsePolicy vnPayIpnResponsePolicy;
    private final ProviderPaymentResultApplier providerPaymentResultApplier;

    public VnPayIpnResult execute(HandleVnPayIpnCommand command) {
        try {
            VnPayPaymentResult result = verifyIpn(command);
            applyPaymentResult(result);
            return vnPayIpnResponsePolicy.success();
        } catch (InvalidVnPaySignatureException | InvalidVnPayPayloadException ex) {
            return vnPayIpnResponsePolicy.signatureFailed();
        } catch (BusinessException ex) {
            return vnPayIpnResponsePolicy.fromBusinessException(ex);
        } catch (RuntimeException ex) {
            return vnPayIpnResponsePolicy.unknownError();
        }
    }

    private VnPayPaymentResult verifyIpn(HandleVnPayIpnCommand command) {
        return vnPayGatewayPort.verifyAndParseIpn(command.params());
    }

    private void applyPaymentResult(VnPayPaymentResult result) {
        providerPaymentResultApplier.apply(
                new ApplyProviderPaymentResultCommand(
                        result.txnRef(),
                        result.amount(),
                        result.isSuccess(),
                        result.responseCode(),
                        result.providerStatusCode(),
                        result.providerTransactionId(),
                        result.rawPayload(),
                        PROVIDER_NAME
                ));
    }
}
