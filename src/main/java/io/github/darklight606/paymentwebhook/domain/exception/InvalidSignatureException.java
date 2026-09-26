package io.github.darklight606.paymentwebhook.domain.exception;

public class InvalidSignatureException extends RuntimeException {

    public InvalidSignatureException(String reason) {
        super(reason);
    }
}
