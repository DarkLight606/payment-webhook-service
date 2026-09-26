package io.github.darklight606.paymentwebhook.adapter.out.acmepay;

import io.github.darklight606.paymentwebhook.config.AcmePayWebhookProperties;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidSignatureException;
import io.github.darklight606.paymentwebhook.domain.port.out.SignatureVerificationPort;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcmePaySignatureAdapter implements SignatureVerificationPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int V1_HEX_LENGTH = 64;

    private final AcmePayWebhookProperties properties;
    private final Clock clock;

    @Override
    public void verify(String signatureHeader, byte[] rawBody) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw fail("signature header missing");
        }

        String timestampValue = null;
        List<String> v1Values = new ArrayList<>();
        for (String part : signatureHeader.split(",", -1)) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex < 0) {
                throw fail("signature header malformed");
            }
            String key = part.substring(0, separatorIndex);
            String value = part.substring(separatorIndex + 1);
            if (key.equals("t")) {
                if (timestampValue != null) {
                    throw fail("signature header malformed");
                }
                timestampValue = value;
            } else if (key.equals("v1")) {
                v1Values.add(value);
            }
        }

        if (timestampValue == null || v1Values.isEmpty()) {
            throw fail("signature header malformed");
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestampValue);
        } catch (NumberFormatException e) {
            throw fail("signature header malformed");
        }

        for (String v1Value : v1Values) {
            if (v1Value.length() != V1_HEX_LENGTH || !isHex(v1Value)) {
                throw fail("signature header malformed");
            }
        }

        Instant timestamp;
        try {
            timestamp = Instant.ofEpochSecond(timestampSeconds);
        } catch (DateTimeException e) {
            throw fail("signature header malformed");
        }
        Duration distance = Duration.between(timestamp, clock.instant()).abs();
        if (distance.compareTo(properties.signatureTolerance()) > 0) {
            throw fail("signature timestamp out of tolerance");
        }

        byte[] expected = hmac(timestampValue, rawBody);
        for (String v1Value : v1Values) {
            byte[] provided = HexFormat.of().parseHex(v1Value);
            if (MessageDigest.isEqual(expected, provided)) {
                return;
            }
        }

        throw fail("signature mismatch");
    }

    private byte[] hmac(String timestampValue, byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update(timestampValue.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            return mac.doFinal(rawBody);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private InvalidSignatureException fail(String reason) {
        LOG.warn("AcmePay webhook signature verification failed [reason={}]", reason);
        return new InvalidSignatureException(reason);
    }
}
