package io.github.darklight606.paymentwebhook.adapter.in.rest;

import io.github.darklight606.paymentwebhook.domain.exception.InvalidPayloadException;
import io.github.darklight606.paymentwebhook.domain.exception.InvalidSignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SIGNATURE_REJECTED_DETAIL = "webhook signature verification failed";

    @ExceptionHandler(InvalidSignatureException.class)
    public ProblemDetail handleInvalidSignature(InvalidSignatureException exception) {
        LOG.warn("AcmePay webhook rejected with invalid signature [reason={}]", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, SIGNATURE_REJECTED_DETAIL);
    }

    @ExceptionHandler(InvalidPayloadException.class)
    public ProblemDetail handleInvalidPayload(InvalidPayloadException exception) {
        LOG.warn("AcmePay webhook rejected with invalid payload [reason={}]", exception.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
