package io.github.darklight606.paymentwebhook.application;

import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;
import io.github.darklight606.paymentwebhook.domain.model.Provider;
import io.github.darklight606.paymentwebhook.domain.model.WebhookEvent;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookCommand;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookResult;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookUseCase;
import io.github.darklight606.paymentwebhook.domain.port.out.SignatureVerificationPort;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookEventRepository;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookPayloadPort;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiveWebhookService implements ReceiveWebhookUseCase {

    private static final String EVENT_ID_MDC_KEY = "eventId";

    private final SignatureVerificationPort signatureVerificationPort;
    private final WebhookPayloadPort webhookPayloadPort;
    private final WebhookEventRepository webhookEventRepository;
    private final Clock clock;

    @Override
    public ReceiveWebhookResult receive(ReceiveWebhookCommand command) {
        signatureVerificationPort.verify(command.signatureHeader(), command.rawBody());
        ParsedWebhookEvent parsed = webhookPayloadPort.parse(command.rawBody());

        try {
            MDC.put(EVENT_ID_MDC_KEY, parsed.eventId());
            LOG.info(
                    "Storing webhook event [provider={}, eventId={}, type={}]",
                    Provider.ACMEPAY,
                    parsed.eventId(),
                    parsed.type());

            WebhookEvent event = WebhookEvent.received(
                    Provider.ACMEPAY,
                    parsed.eventId(),
                    parsed.type(),
                    parsed.paymentReference(),
                    parsed.amountMinor(),
                    parsed.currency(),
                    new String(command.rawBody(), StandardCharsets.UTF_8),
                    clock.instant());

            boolean inserted = webhookEventRepository.saveIfAbsent(event);
            if (inserted) {
                LOG.info(
                        "Webhook event stored [provider={}, eventId={}, type={}]",
                        Provider.ACMEPAY,
                        parsed.eventId(),
                        parsed.type());
            } else {
                LOG.info(
                        "Duplicate webhook event ignored [provider={}, eventId={}]",
                        Provider.ACMEPAY,
                        parsed.eventId());
            }

            return new ReceiveWebhookResult(parsed.eventId(), !inserted);
        } finally {
            MDC.remove(EVENT_ID_MDC_KEY);
        }
    }
}
