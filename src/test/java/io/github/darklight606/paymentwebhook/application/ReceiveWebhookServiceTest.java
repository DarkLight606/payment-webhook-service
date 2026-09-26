package io.github.darklight606.paymentwebhook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidSignatureException;
import io.github.darklight606.paymentwebhook.domain.model.EventType;
import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;
import io.github.darklight606.paymentwebhook.domain.model.Provider;
import io.github.darklight606.paymentwebhook.domain.model.WebhookEvent;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookCommand;
import io.github.darklight606.paymentwebhook.domain.port.in.ReceiveWebhookResult;
import io.github.darklight606.paymentwebhook.domain.port.out.SignatureVerificationPort;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookEventRepository;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookPayloadPort;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class ReceiveWebhookServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-26T00:00:00Z");
    private static final byte[] RAW_BODY = "{\"id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);
    private static final ParsedWebhookEvent PARSED =
            new ParsedWebhookEvent("evt_1", EventType.PAYMENT_SUCCEEDED, "pay_1", 1500L, "EUR");

    @Mock
    private SignatureVerificationPort signatureVerificationPort;

    @Mock
    private WebhookPayloadPort webhookPayloadPort;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private ReceiveWebhookService service() {
        return new ReceiveWebhookService(signatureVerificationPort, webhookPayloadPort, webhookEventRepository, clock);
    }

    @Test
    void receive_validRequest_savesEventAndReturnsNotDuplicate() {
        when(webhookPayloadPort.parse(RAW_BODY)).thenReturn(PARSED);
        when(webhookEventRepository.saveIfAbsent(any())).thenReturn(true);

        ReceiveWebhookResult result = service().receive(new ReceiveWebhookCommand("header", RAW_BODY));

        assertThat(result.eventId()).isEqualTo("evt_1");
        assertThat(result.isDuplicate()).isFalse();

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).saveIfAbsent(captor.capture());
        WebhookEvent saved = captor.getValue();
        assertThat(saved.provider()).isEqualTo(Provider.ACMEPAY);
        assertThat(saved.eventId()).isEqualTo("evt_1");
        assertThat(saved.type()).isEqualTo(EventType.PAYMENT_SUCCEEDED);
        assertThat(saved.paymentReference()).isEqualTo("pay_1");
        assertThat(saved.amountMinor()).isEqualTo(1500L);
        assertThat(saved.currency()).isEqualTo("EUR");
        assertThat(saved.rawPayload()).isEqualTo(new String(RAW_BODY, StandardCharsets.UTF_8));
        assertThat(saved.receivedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void receive_duplicateEvent_returnsIsDuplicateTrue() {
        when(webhookPayloadPort.parse(RAW_BODY)).thenReturn(PARSED);
        when(webhookEventRepository.saveIfAbsent(any())).thenReturn(false);

        ReceiveWebhookResult result = service().receive(new ReceiveWebhookCommand("header", RAW_BODY));

        assertThat(result.isDuplicate()).isTrue();
    }

    @Test
    void receive_invalidSignature_propagatesAndSkipsParseAndSave() {
        var command = new ReceiveWebhookCommand("bad-header", RAW_BODY);
        doThrow(new InvalidSignatureException("signature mismatch"))
                .when(signatureVerificationPort)
                .verify("bad-header", RAW_BODY);

        assertThatThrownBy(() -> service().receive(command)).isInstanceOf(InvalidSignatureException.class);

        verifyNoInteractions(webhookPayloadPort);
        verifyNoInteractions(webhookEventRepository);
    }

    @Test
    void receive_invalidPayload_propagatesAndSkipsSave() {
        var command = new ReceiveWebhookCommand("header", RAW_BODY);
        when(webhookPayloadPort.parse(RAW_BODY)).thenThrow(new InvalidPayloadException("payload invalid"));

        assertThatThrownBy(() -> service().receive(command)).isInstanceOf(InvalidPayloadException.class);

        verify(webhookEventRepository, never()).saveIfAbsent(any());
    }

    @Test
    void receive_afterCall_mdcHoldsNoEventId() {
        when(webhookPayloadPort.parse(RAW_BODY)).thenReturn(PARSED);
        when(webhookEventRepository.saveIfAbsent(any())).thenReturn(true);

        service().receive(new ReceiveWebhookCommand("header", RAW_BODY));

        assertThat(MDC.get("eventId")).isNull();
    }
}
