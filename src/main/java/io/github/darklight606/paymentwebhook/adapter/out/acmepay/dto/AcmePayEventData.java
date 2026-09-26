package io.github.darklight606.paymentwebhook.adapter.out.acmepay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AcmePayEventData(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") @JsonProperty("payment_id")
        String paymentId,

        @NotNull @Min(1) Long amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency) {}
