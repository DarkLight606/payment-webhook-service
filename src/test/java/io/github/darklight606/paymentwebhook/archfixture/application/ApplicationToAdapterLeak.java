package io.github.darklight606.paymentwebhook.archfixture.application;

import io.github.darklight606.paymentwebhook.archfixture.adapter.AdapterMarker;

/** Fixture that violates the application/adapter-independence rule on purpose. */
class ApplicationToAdapterLeak {
    private AdapterMarker marker;
}
