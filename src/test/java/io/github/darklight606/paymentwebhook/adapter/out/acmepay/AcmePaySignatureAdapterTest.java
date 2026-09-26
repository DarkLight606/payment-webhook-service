package io.github.darklight606.paymentwebhook.adapter.out.acmepay;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.darklight606.paymentwebhook.AcmePaySignatureTestHelper;
import io.github.darklight606.paymentwebhook.config.AcmePayWebhookProperties;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidSignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.unit.DataSize;

class AcmePaySignatureAdapterTest {

    private static final String SECRET = "acmepay-test-secret";
    private static final Instant FIXED_NOW = Instant.parse("2026-09-26T00:00:00Z");
    private static final byte[] BODY =
            "{\"id\":\"evt_1\",\"type\":\"payment.succeeded\"}".getBytes(StandardCharsets.UTF_8);

    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final AcmePayWebhookProperties properties =
            new AcmePayWebhookProperties(SECRET, Duration.ofMinutes(5), DataSize.ofKilobytes(16));
    private final AcmePaySignatureAdapter adapter = new AcmePaySignatureAdapter(properties, clock);

    @Test
    void verify_validSignature_passes() {
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, BODY);

        adapter.verify(header, BODY);
    }

    @Test
    void verify_knownAnswerVector_passes() {
        byte[] body = ("{\"id\":\"evt_kat_1\",\"type\":\"payment.succeeded\",\"data\":"
                        + "{\"payment_id\":\"pay_kat_1\",\"amount\":1500,\"currency\":\"EUR\"}}")
                .getBytes(StandardCharsets.UTF_8);
        String header = "t=1790000000,v1=cf1f464f2e956ec0689bad14f1fca579df573f26399e9b09e8ba414ca76f3476";
        var katClock = Clock.fixed(Instant.ofEpochSecond(1_790_000_000), ZoneOffset.UTC);
        var katAdapter = new AcmePaySignatureAdapter(properties, katClock);

        katAdapter.verify(header, body);
    }

    @Test
    void verify_wrongSecret_throws() {
        var wrongProperties =
                new AcmePayWebhookProperties("wrong-secret", Duration.ofMinutes(5), DataSize.ofKilobytes(16));
        var wrongAdapter = new AcmePaySignatureAdapter(wrongProperties, clock);
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, BODY);

        assertThatThrownBy(() -> wrongAdapter.verify(header, BODY)).isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void verify_changedBodyByte_throws() {
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, BODY);
        byte[] tamperedBody = BODY.clone();
        tamperedBody[0] = (byte) '[';

        assertThatThrownBy(() -> adapter.verify(header, tamperedBody)).isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void verify_changedTimestamp_throws() {
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, BODY);
        String tamperedHeader =
                header.replace("t=" + FIXED_NOW.getEpochSecond(), "t=" + (FIXED_NOW.getEpochSecond() + 1));

        assertThatThrownBy(() -> adapter.verify(tamperedHeader, BODY)).isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void verify_staleTimestampInPast_throws() {
        Instant stale = FIXED_NOW.minus(Duration.ofMinutes(5)).minusSeconds(1);
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, stale, BODY);

        assertThatThrownBy(() -> adapter.verify(header, BODY)).isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void verify_staleTimestampInFuture_throws() {
        Instant stale = FIXED_NOW.plus(Duration.ofMinutes(5)).plusSeconds(1);
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, stale, BODY);

        assertThatThrownBy(() -> adapter.verify(header, BODY)).isInstanceOf(InvalidSignatureException.class);
    }

    @Test
    void verify_timestampExactlyAtTolerance_passes() {
        Instant atTolerance = FIXED_NOW.minus(Duration.ofMinutes(5));
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, atTolerance, BODY);

        adapter.verify(header, BODY);
    }

    @Test
    void verify_wrongV1FollowedByRightV1_passes() {
        String header = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, BODY);
        String wrongV1 = "v1=" + "0".repeat(64);
        String combinedHeader = header.replaceFirst("v1=", wrongV1 + ",v1=");

        adapter.verify(combinedHeader, BODY);
    }

    @ParameterizedTest
    @MethodSource("malformedHeaders")
    void verify_malformedHeader_throws(String header) {
        assertThatThrownBy(() -> adapter.verify(header, BODY)).isInstanceOf(InvalidSignatureException.class);
    }

    private static Stream<String> malformedHeaders() {
        String validV1 = "cf1f464f2e956ec0689bad14f1fca579df573f26399e9b09e8ba414ca76f3476";
        return Stream.of(
                null,
                "",
                "   ",
                "v1=" + validV1,
                "t=1790000000,t=1790000001,v1=" + validV1,
                "t=not-a-number,v1=" + validV1,
                "t=1790000000",
                "t=1790000000,v1=short",
                "t=1790000000,v1=" + "g".repeat(64),
                "t=1790000000,v1=" + "０".repeat(64),
                "t=1790000000;v1=" + validV1);
    }
}
