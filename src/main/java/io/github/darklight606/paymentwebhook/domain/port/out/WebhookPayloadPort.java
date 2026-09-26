package io.github.darklight606.paymentwebhook.domain.port.out;

import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;

public interface WebhookPayloadPort {

    ParsedWebhookEvent parse(byte[] rawBody);
}
