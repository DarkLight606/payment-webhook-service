package io.github.darklight606.paymentwebhook.adapter.out.acmepay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.model.EventType;
import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AcmePayPayloadAdapterTest {

    private final AcmePayPayloadAdapter adapter = new AcmePayPayloadAdapter(
            new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void parse_paymentSucceeded_mapsFields() {
        ParsedWebhookEvent event = adapter.parse(body("{\"id\":\"evt_1\",\"type\":\"payment.succeeded\","
                + "\"data\":{\"payment_id\":\"pay_1\",\"amount\":1500,\"currency\":\"EUR\"}}"));

        assertThat(event.eventId()).isEqualTo("evt_1");
        assertThat(event.type()).isEqualTo(EventType.PAYMENT_SUCCEEDED);
        assertThat(event.paymentReference()).isEqualTo("pay_1");
        assertThat(event.amountMinor()).isEqualTo(1500L);
        assertThat(event.currency()).isEqualTo("EUR");
    }

    @Test
    void parse_paymentFailed_mapsEventType() {
        ParsedWebhookEvent event = adapter.parse(body("{\"id\":\"evt_2\",\"type\":\"payment.failed\","
                + "\"data\":{\"payment_id\":\"pay_2\",\"amount\":500,\"currency\":\"USD\"}}"));

        assertThat(event.type()).isEqualTo(EventType.PAYMENT_FAILED);
    }

    @Test
    void parse_refundSucceeded_mapsEventType() {
        ParsedWebhookEvent event = adapter.parse(body("{\"id\":\"evt_3\",\"type\":\"refund.succeeded\","
                + "\"data\":{\"payment_id\":\"pay_3\",\"amount\":250,\"currency\":\"GBP\"}}"));

        assertThat(event.type()).isEqualTo(EventType.REFUND_SUCCEEDED);
    }

    @Test
    void parse_unknownFieldPresent_ignoresIt() {
        ParsedWebhookEvent event =
                adapter.parse(body("{\"id\":\"evt_4\",\"type\":\"payment.succeeded\",\"unexpected\":\"value\","
                        + "\"data\":{\"payment_id\":\"pay_4\",\"amount\":100,\"currency\":\"EUR\",\"extra\":1}}"));

        assertThat(event.eventId()).isEqualTo("evt_4");
    }

    @ParameterizedTest
    @MethodSource("invalidPayloads")
    void parse_invalidPayload_throws(byte[] rawBody) {
        assertThatThrownBy(() -> adapter.parse(rawBody)).isInstanceOf(InvalidPayloadException.class);
    }

    private static Stream<byte[]> invalidPayloads() {
        return Stream.of(
                body(""),
                body("not json"),
                body("[]"),
                body("{\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":100,\"currency\":\"EUR\"}}"),
                body("{\"id\":\"evt_1\\n\",\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":100,\"currency\":\"EUR\"}}"),
                body("{\"id\":\"evt_1\",\"type\":\"payment.succeeded\"}"),
                body("{\"id\":\"evt_1\",\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"currency\":\"EUR\"}}"),
                body("{\"id\":\"evt_1\",\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":0,\"currency\":\"EUR\"}}"),
                body("{\"id\":\"evt_1\",\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":100,\"currency\":\"eur\"}}"),
                body("{\"id\":\"evt_1\",\"type\":\"payment.refunded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":100,\"currency\":\"EUR\"}}"));
    }

    private static byte[] body(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
