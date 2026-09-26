package io.github.darklight606.paymentwebhook.domain.port.in;

public record ReceiveWebhookCommand(String signatureHeader, byte[] rawBody) {}
