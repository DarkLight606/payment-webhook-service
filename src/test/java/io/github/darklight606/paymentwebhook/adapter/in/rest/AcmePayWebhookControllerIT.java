package io.github.darklight606.paymentwebhook.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.darklight606.paymentwebhook.AcmePaySignatureTestHelper;
import io.github.darklight606.paymentwebhook.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AcmePayWebhookControllerIT {

    private static final String SIGNATURE_HEADER_NAME = "AcmePay-Signature";
    private static final String SECRET = "acmepay-test-secret";
    private static final Instant FIXED_NOW = Instant.parse("2026-09-26T00:00:00Z");
    private static final String VALID_BODY = "{\"id\":\"evt_1\",\"type\":\"payment.succeeded\",\"data\":"
            + "{\"payment_id\":\"pay_1\",\"amount\":1500,\"currency\":\"EUR\"}}";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("delete from webhook_event").update();
    }

    @Test
    void receive_validEvent_returns200AndStoresRow() {
        byte[] body = VALID_BODY.getBytes(StandardCharsets.UTF_8);
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);

        ResponseEntity<Void> response = post(body, signature);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var row = jdbcClient
                .sql("select * from webhook_event where provider = 'ACMEPAY' and event_id = 'evt_1'")
                .query()
                .singleRow();
        assertThat(row.get("raw_payload")).isEqualTo(VALID_BODY);
        assertThat(row.get("status")).isEqualTo("RECEIVED");
        var receivedAt = jdbcClient
                .sql("select received_at from webhook_event where provider = 'ACMEPAY' and event_id = 'evt_1'")
                .query(OffsetDateTime.class)
                .single();
        assertThat(receivedAt.toInstant()).isEqualTo(FIXED_NOW);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRequests")
    void receive_unauthorizedRequest_returns401AndStoresNothing(String signatureHeader, byte[] body) {
        ResponseEntity<Void> response = post(body, signatureHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rowCount()).isZero();
    }

    private static Stream<Arguments> unauthorizedRequests() {
        byte[] body = VALID_BODY.getBytes(StandardCharsets.UTF_8);
        String forged = AcmePaySignatureTestHelper.validHeader("wrong-secret", FIXED_NOW, body);
        String malformed = "not-a-signature";
        String stale = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW.minus(Duration.ofMinutes(6)), body);
        return Stream.of(
                Arguments.of(forged, body),
                Arguments.of(null, body),
                Arguments.of(malformed, body),
                Arguments.of(stale, body));
    }

    @Test
    void receive_noBody_returns400AndStoresNothing() {
        byte[] emptyBody = new byte[0];
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, emptyBody);
        HttpHeaders headers = new HttpHeaders();
        headers.add(SIGNATURE_HEADER_NAME, signature);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/webhooks/acmepay", HttpMethod.POST, new HttpEntity<>(null, headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rowCount()).isZero();
    }

    @Test
    void receive_oversizedBody_returns413AndStoresNothing() {
        byte[] body = new byte[17 * 1024];
        Arrays.fill(body, (byte) 'a');
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);

        ResponseEntity<Void> response = post(body, signature);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(rowCount()).isZero();
    }

    @Test
    void receive_invalidJson_returns400AndStoresNothing() {
        byte[] body = "not json".getBytes(StandardCharsets.UTF_8);
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);

        ResponseEntity<Void> response = post(body, signature);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rowCount()).isZero();
    }

    @Test
    void receive_unknownEventType_returns400AndStoresNothing() {
        byte[] body = ("{\"id\":\"evt_1\",\"type\":\"payment.refunded\",\"data\":"
                        + "{\"payment_id\":\"pay_1\",\"amount\":1500,\"currency\":\"EUR\"}}")
                .getBytes(StandardCharsets.UTF_8);
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);

        ResponseEntity<Void> response = post(body, signature);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rowCount()).isZero();
    }

    @Test
    void receive_sameEventTwice_returns200BothTimesAndKeepsOneRow() {
        byte[] body = VALID_BODY.getBytes(StandardCharsets.UTF_8);
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);

        ResponseEntity<Void> first = post(body, signature);
        ResponseEntity<Void> second = post(body, signature);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rowCount()).isEqualTo(1L);
    }

    @Test
    void receive_tenParallelDeliveriesOfSameEvent_returnTenOkAndOneRow() throws InterruptedException {
        byte[] body = VALID_BODY.getBytes(StandardCharsets.UTF_8);
        String signature = AcmePaySignatureTestHelper.validHeader(SECRET, FIXED_NOW, body);
        int requestCount = 10;
        var startLatch = new CountDownLatch(1);
        var doneLatch = new CountDownLatch(requestCount);
        var okCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);

        try {
            for (int i = 0; i < requestCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        if (post(body, signature).getStatusCode() == HttpStatus.OK) {
                            okCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdown();
        }

        assertThat(okCount.get()).isEqualTo(requestCount);
        assertThat(rowCount()).isEqualTo(1L);
    }

    private ResponseEntity<Void> post(byte[] body, String signatureHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (signatureHeader != null) {
            headers.add(SIGNATURE_HEADER_NAME, signatureHeader);
        }
        return restTemplate.exchange("/webhooks/acmepay", HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
    }

    private long rowCount() {
        return jdbcClient
                .sql("select count(*) from webhook_event")
                .query(Long.class)
                .single();
    }
}
