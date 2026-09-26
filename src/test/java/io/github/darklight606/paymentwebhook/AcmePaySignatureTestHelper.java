package io.github.darklight606.paymentwebhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AcmePaySignatureTestHelper {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private AcmePaySignatureTestHelper() {}

    public static String validHeader(String secret, Instant timestamp, byte[] rawBody) {
        String timestampValue = String.valueOf(timestamp.getEpochSecond());
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update(timestampValue.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            byte[] hmac = mac.doFinal(rawBody);
            return "t=" + timestampValue + ",v1=" + HexFormat.of().formatHex(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
