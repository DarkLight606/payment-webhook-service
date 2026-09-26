package io.github.darklight606.paymentwebhook.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "acmepay.webhook")
public record AcmePayWebhookProperties(
        @NotBlank String secret, @NotNull Duration signatureTolerance) {}
