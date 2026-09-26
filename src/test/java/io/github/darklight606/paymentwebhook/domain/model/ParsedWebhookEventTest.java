package io.github.darklight606.paymentwebhook.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParsedWebhookEventTest {

    @Test
    void constructor_givenAllFields_keepsEveryField() {
        var parsed = new ParsedWebhookEvent("evt_123", EventType.PAYMENT_SUCCEEDED, "pay_456", 1_500L, "EUR");

        assertThat(parsed.eventId()).isEqualTo("evt_123");
        assertThat(parsed.type()).isEqualTo(EventType.PAYMENT_SUCCEEDED);
        assertThat(parsed.paymentReference()).isEqualTo("pay_456");
        assertThat(parsed.amountMinor()).isEqualTo(1_500L);
        assertThat(parsed.currency()).isEqualTo("EUR");
    }
}
