package io.github.darklight606.paymentwebhook.adapter.in.rest;

import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookCommand;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AcmePayWebhookController {

    private static final String SIGNATURE_HEADER_NAME = "AcmePay-Signature";
    private static final byte[] EMPTY_BODY = new byte[0];

    private final ReceiveWebhookUseCase receiveWebhookUseCase;

    @PostMapping("/webhooks/acmepay")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = SIGNATURE_HEADER_NAME, required = false) String signatureHeader,
            @RequestBody(required = false) byte[] rawBody) {
        receiveWebhookUseCase.receive(
                new ReceiveWebhookCommand(signatureHeader, rawBody == null ? EMPTY_BODY : rawBody));
        return ResponseEntity.ok().build();
    }
}
