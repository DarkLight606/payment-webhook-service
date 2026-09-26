package io.github.darklight606.paymentwebhook.domain.exception;

public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(String reason) {
        super(reason);
    }
}
