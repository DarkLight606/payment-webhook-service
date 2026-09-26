package io.github.darklight606.paymentwebhook.domain.port.out;

import io.github.darklight606.paymentwebhook.domain.model.WebhookEvent;

public interface WebhookEventRepository {

    boolean saveIfAbsent(WebhookEvent event);
}
