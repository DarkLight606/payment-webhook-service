package io.github.darklight606.paymentwebhook.archfixture.domain;

import io.github.darklight606.paymentwebhook.archfixture.application.ApplicationMarker;

/** Fixture that violates the domain/application-independence rule on purpose. */
class DomainToApplicationLeak {
    private ApplicationMarker marker;
}
