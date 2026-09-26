package io.github.darklight606.paymentwebhook.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.darklight606.paymentwebhook.TestcontainersConfiguration;
import io.github.darklight606.paymentwebhook.domain.model.EventType;
import io.github.darklight606.paymentwebhook.domain.model.Provider;
import io.github.darklight606.paymentwebhook.domain.model.WebhookEvent;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class WebhookEventRepositoryAdapterIT {

    @Autowired
    private WebhookEventRepositoryAdapter repositoryAdapter;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void emptyTable() {
        jdbcClient.sql("delete from webhook_event").update();
    }

    @Test
    void saveIfAbsent_newEvent_returnsTrueAndStoresEveryField() {
        var receivedAt = Instant.parse("2026-09-26T12:00:00Z");
        var event = WebhookEvent.received(
                Provider.ACMEPAY,
                "evt_1",
                EventType.PAYMENT_SUCCEEDED,
                "pay_1",
                1500L,
                "EUR",
                "{\"id\":\"evt_1\"}",
                receivedAt);

        boolean inserted = repositoryAdapter.saveIfAbsent(event);

        assertThat(inserted).isTrue();
        var row = jdbcClient
                .sql("select * from webhook_event where provider = :provider and event_id = :eventId")
                .param("provider", "ACMEPAY")
                .param("eventId", "evt_1")
                .query()
                .singleRow();
        assertThat(row.get("event_type")).isEqualTo("PAYMENT_SUCCEEDED");
        assertThat(row.get("payment_reference")).isEqualTo("pay_1");
        assertThat(row.get("amount_minor")).isEqualTo(1500L);
        assertThat(row.get("currency")).isEqualTo("EUR");
        assertThat(row.get("raw_payload")).isEqualTo("{\"id\":\"evt_1\"}");
        assertThat(row.get("status")).isEqualTo("RECEIVED");
    }

    @Test
    void saveIfAbsent_duplicateKey_returnsFalseAndKeepsFirstAmount() {
        var receivedAt = Instant.parse("2026-09-26T12:00:00Z");
        var first = WebhookEvent.received(
                Provider.ACMEPAY,
                "evt_2",
                EventType.PAYMENT_SUCCEEDED,
                "pay_2",
                1500L,
                "EUR",
                "{\"id\":\"evt_2\"}",
                receivedAt);
        var second = WebhookEvent.received(
                Provider.ACMEPAY,
                "evt_2",
                EventType.PAYMENT_SUCCEEDED,
                "pay_2",
                9999L,
                "EUR",
                "{\"id\":\"evt_2\",\"amount\":9999}",
                receivedAt);

        boolean firstInserted = repositoryAdapter.saveIfAbsent(first);
        boolean secondInserted = repositoryAdapter.saveIfAbsent(second);

        assertThat(firstInserted).isTrue();
        assertThat(secondInserted).isFalse();
        var amount = jdbcClient
                .sql("select amount_minor from webhook_event where provider = :provider and event_id = :eventId")
                .param("provider", "ACMEPAY")
                .param("eventId", "evt_2")
                .query(Long.class)
                .single();
        assertThat(amount).isEqualTo(1500L);
    }

    @Test
    void saveIfAbsent_tenConcurrentSaves_yieldExactlyOneTrueAndOneRow() throws InterruptedException {
        var receivedAt = Instant.parse("2026-09-26T12:00:00Z");
        int threadCount = 10;
        var startLatch = new CountDownLatch(1);
        var doneLatch = new CountDownLatch(threadCount);
        var trueCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        var event = WebhookEvent.received(
                                Provider.ACMEPAY,
                                "evt_3",
                                EventType.PAYMENT_SUCCEEDED,
                                "pay_3",
                                1500L,
                                "EUR",
                                "{\"id\":\"evt_3\"}",
                                receivedAt);
                        if (repositoryAdapter.saveIfAbsent(event)) {
                            trueCount.incrementAndGet();
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

        assertThat(trueCount.get()).isEqualTo(1);
        var rowCount = jdbcClient
                .sql("select count(*) from webhook_event where provider = :provider and event_id = :eventId")
                .param("provider", "ACMEPAY")
                .param("eventId", "evt_3")
                .query(Long.class)
                .single();
        assertThat(rowCount).isEqualTo(1L);
    }
}
