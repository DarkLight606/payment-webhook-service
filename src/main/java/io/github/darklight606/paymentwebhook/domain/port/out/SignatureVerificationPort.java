package io.github.darklight606.paymentwebhook.domain.port.out;

public interface SignatureVerificationPort {

    void verify(String signatureHeader, byte[] rawBody);
}
