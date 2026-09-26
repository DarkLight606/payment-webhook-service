package io.github.darklight606.paymentwebhook.domain.port.in;

public interface ReceiveWebhookUseCase {

    ReceiveWebhookResult receive(ReceiveWebhookCommand command);
}
