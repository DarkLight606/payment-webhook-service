package io.github.darklight606.paymentwebhook.adapter.out.persistence;

import io.github.darklight606.paymentwebhook.domain.model.WebhookEvent;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WebhookEventRepositoryAdapter implements WebhookEventRepository {

    private static final String INSERT_IF_ABSENT_SQL = """
            insert into webhook_event
                (provider, event_id, event_type, payment_reference, amount_minor, currency, raw_payload, received_at, status)
            values
                (:provider, :eventId, :eventType, :paymentReference, :amountMinor, :currency, :rawPayload, :receivedAt, :status)
            on conflict (provider, event_id) do nothing
            """;

    private final JdbcClient jdbcClient;

    @Override
    public boolean saveIfAbsent(WebhookEvent event) {
        int updated = jdbcClient
                .sql(INSERT_IF_ABSENT_SQL)
                .param("provider", event.provider().name())
                .param("eventId", event.eventId())
                .param("eventType", event.type().name())
                .param("paymentReference", event.paymentReference())
                .param("amountMinor", event.amountMinor())
                .param("currency", event.currency())
                .param("rawPayload", event.rawPayload())
                .param("receivedAt", OffsetDateTime.ofInstant(event.receivedAt(), ZoneOffset.UTC))
                .param("status", event.status().name())
                .update();
        return updated == 1;
    }
}
