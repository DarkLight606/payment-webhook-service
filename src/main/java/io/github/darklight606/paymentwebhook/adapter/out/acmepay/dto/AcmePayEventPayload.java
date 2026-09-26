package io.github.darklight606.paymentwebhook.adapter.out.acmepay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AcmePayEventPayload(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$") String id,
        @NotBlank String type,
        @NotNull @Valid AcmePayEventData data) {}
