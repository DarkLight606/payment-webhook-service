package io.github.darklight606.paymentwebhook.adapter.out.acmepay;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.darklight606.paymentwebhook.adapter.out.acmepay.dto.AcmePayEventPayload;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.model.ParsedWebhookEvent;
import io.github.darklight606.paymentwebhook.domain.port.out.WebhookPayloadPort;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcmePayPayloadAdapter implements WebhookPayloadPort {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AcmePayEventMapper eventMapper = new AcmePayEventMapper();

    @Override
    public ParsedWebhookEvent parse(byte[] rawBody) {
        AcmePayEventPayload payload = readPayload(rawBody);
        validate(payload);
        try {
            return eventMapper.toDomain(payload);
        } catch (InvalidPayloadException e) {
            throw fail(e.getMessage());
        }
    }

    private AcmePayEventPayload readPayload(byte[] rawBody) {
        try {
            return objectMapper
                    .readerFor(AcmePayEventPayload.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(rawBody);
        } catch (IOException e) {
            throw fail("webhook payload is not valid json");
        }
    }

    private void validate(AcmePayEventPayload payload) {
        if (payload == null) {
            throw fail("webhook payload is not valid json");
        }
        Set<ConstraintViolation<AcmePayEventPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            throw fail("webhook payload failed validation");
        }
    }

    private InvalidPayloadException fail(String reason) {
        LOG.warn("AcmePay webhook payload rejected [reason={}]", reason);
        return new InvalidPayloadException(reason);
    }
}
