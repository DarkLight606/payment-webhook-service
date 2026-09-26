package io.github.darklight606.paymentwebhook.domain.port.in;

public record ReceiveWebhookResult(String eventId, boolean isDuplicate) {}
