package io.github.darklight606.paymentwebhook.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvalidPayloadExceptionTest {

    @Test
    void constructor_givenReason_keepsReasonAsMessage() {
        var exception = new InvalidPayloadException("body is not valid JSON");

        assertThat(exception.getMessage()).isEqualTo("body is not valid JSON");
    }
}
