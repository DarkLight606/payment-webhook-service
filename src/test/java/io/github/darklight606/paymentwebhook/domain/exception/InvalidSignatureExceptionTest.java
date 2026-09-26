package io.github.darklight606.paymentwebhook.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvalidSignatureExceptionTest {

    @Test
    void constructor_givenReason_keepsReasonAsMessage() {
        var exception = new InvalidSignatureException("signature header missing");

        assertThat(exception.getMessage()).isEqualTo("signature header missing");
    }
}
