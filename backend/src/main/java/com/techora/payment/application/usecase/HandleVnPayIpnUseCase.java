package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.payment.application.command.HandleVnPayIpnCommand;
import com.techora.payment.application.command.ProcessPaymentResultCommand;
import com.techora.payment.application.exception.InvalidVnPayPayloadException;
import com.techora.payment.application.exception.InvalidVnPaySignatureException;
import com.techora.payment.application.model.VnPayIpnReply;
import com.techora.payment.application.model.WebhookProcessResult;
import com.techora.payment.application.policy.VnPayIpnReplyPolicy;
import com.techora.payment.application.port.gateway.VerifiedVnPayIpn;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.service.PaymentWebhookProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HandleVnPayIpnUseCase {

    private final VnPayGatewayPort vnPayGatewayPort;
    private final VnPayIpnReplyPolicy vnPayIpnReplyPolicy;
    private final PaymentWebhookProcessor webhookProcessor;
    private final Clock clock;

    public VnPayIpnReply execute(HandleVnPayIpnCommand command) {
        try {
            VerifiedVnPayIpn verifiedIpn = verifyIpn(command);
            WebhookProcessResult result = processWebhook(verifiedIpn);

            return mapToVnPayReply(result);
        } catch (InvalidVnPaySignatureException | InvalidVnPayPayloadException ex) {
            return vnPayIpnReplyPolicy.signatureFailed();
        } catch (BusinessException ex) {
            return vnPayIpnReplyPolicy.fromBusinessException(ex);
        } catch (RuntimeException ex) {
            return vnPayIpnReplyPolicy.unknownError();
        }
    }

    private VerifiedVnPayIpn verifyIpn(HandleVnPayIpnCommand command) {
        return vnPayGatewayPort.verifyAndParseIpn(command.params());
    }

    private WebhookProcessResult processWebhook(VerifiedVnPayIpn verifiedIpn) {
        Instant receivedAt = now();
        return webhookProcessor.process(
                new ProcessPaymentResultCommand(
                        verifiedIpn.txnRef(),
                        verifiedIpn.amount(),
                        verifiedIpn.isSuccess(),
                        verifiedIpn.responseCode(),
                        verifiedIpn.providerStatusCode(),
                        verifiedIpn.providerTransactionId(),
                        verifiedIpn.rawPayload(),
                        receivedAt
                ));
    }

    private VnPayIpnReply mapToVnPayReply(WebhookProcessResult result) {
        return switch (result) {
            case SUCCESS, ALREADY_HANDLED -> vnPayIpnReplyPolicy.success();
            case AMOUNT_MISMATCH -> vnPayIpnReplyPolicy.invalidAmount();
            case PAYMENT_NOT_FOUND -> vnPayIpnReplyPolicy.orderNotFound();
        };
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
