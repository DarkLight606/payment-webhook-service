package io.github.darklight606.paymentwebhook.domain.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReceiveWebhookResultTest {

    @Test
    void constructor_givenEventIdAndDuplicateFlag_keepsBothFields() {
        var result = new ReceiveWebhookResult("evt_123", true);

        assertThat(result.eventId()).isEqualTo("evt_123");
        assertThat(result.isDuplicate()).isTrue();
    }
}
