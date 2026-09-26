package io.github.darklight606.paymentwebhook.domain.model;

public record ParsedWebhookEvent(
        String eventId, EventType type, String paymentReference, long amountMinor, String currency) {}
