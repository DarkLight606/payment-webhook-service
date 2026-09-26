package io.github.darklight606.paymentwebhook.domain.model;

import java.time.Instant;

public record WebhookEvent(
        Provider provider,
        String eventId,
        EventType type,
        String paymentReference,
        long amountMinor,
        String currency,
        String rawPayload,
        Instant receivedAt,
        EventStatus status) {

    public static WebhookEvent received(
            Provider provider,
            String eventId,
            EventType type,
            String paymentReference,
            long amountMinor,
            String currency,
            String rawPayload,
            Instant receivedAt) {
        return new WebhookEvent(
                provider,
                eventId,
                type,
                paymentReference,
                amountMinor,
                currency,
                rawPayload,
                receivedAt,
                EventStatus.RECEIVED);
    }
}
