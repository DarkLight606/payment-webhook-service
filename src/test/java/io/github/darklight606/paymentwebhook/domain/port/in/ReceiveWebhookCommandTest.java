package io.github.darklight606.paymentwebhook.domain.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReceiveWebhookCommandTest {

    @Test
    void constructor_givenSignatureAndBody_keepsBothFields() {
        var body = "{}".getBytes(StandardCharsets.UTF_8);

        var command = new ReceiveWebhookCommand("t=1,v1=abc", body);

        assertThat(command.signatureHeader()).isEqualTo("t=1,v1=abc");
        assertThat(command.rawBody()).isEqualTo(body);
    }
}
