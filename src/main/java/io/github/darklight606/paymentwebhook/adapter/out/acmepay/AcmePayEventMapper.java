package io.github.darklight606.paymentwebhook.adapter.out.acmepay;

import io.github.darklight606.paymentwebhook.adapter.out.acmepay.dto.AcmePayEventPayload;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.model.EventType;
import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;

class AcmePayEventMapper {

    ParsedWebhookEvent toDomain(AcmePayEventPayload payload) {
        EventType type = toEventType(payload.type());
        return new ParsedWebhookEvent(
                payload.id(),
                type,
                payload.data().paymentId(),
                payload.data().amount(),
                payload.data().currency());
    }

    private EventType toEventType(String type) {
        return switch (type) {
            case "payment.succeeded" -> EventType.PAYMENT_SUCCEEDED;
            case "payment.failed" -> EventType.PAYMENT_FAILED;
            case "refund.succeeded" -> EventType.REFUND_SUCCEEDED;
            default -> throw new InvalidPayloadException("unknown event type");
        };
    }
}
