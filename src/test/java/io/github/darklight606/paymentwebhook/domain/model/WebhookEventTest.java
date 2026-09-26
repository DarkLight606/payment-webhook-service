package io.github.darklight606.paymentwebhook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WebhookEventTest {

    @Test
    void received_givenAllFields_setsStatusReceivedAndKeepsEveryField() {
        var receivedAt = Instant.parse("2026-09-26T10:00:00Z");

        var event = WebhookEvent.received(
                Provider.ACMEPAY,
                "evt_123",
                EventType.PAYMENT_SUCCEEDED,
                "pay_456",
                1_500L,
                "EUR",
                "{\"id\":\"evt_123\"}",
                receivedAt);

        assertThat(event.provider()).isEqualTo(Provider.ACMEPAY);
        assertThat(event.eventId()).isEqualTo("evt_123");
        assertThat(event.type()).isEqualTo(EventType.PAYMENT_SUCCEEDED);
        assertThat(event.paymentReference()).isEqualTo("pay_456");
        assertThat(event.amountMinor()).isEqualTo(1_500L);
        assertThat(event.currency()).isEqualTo("EUR");
        assertThat(event.rawPayload()).isEqualTo("{\"id\":\"evt_123\"}");
        assertThat(event.receivedAt()).isEqualTo(receivedAt);
        assertThat(event.status()).isEqualTo(EventStatus.RECEIVED);
    }
}
