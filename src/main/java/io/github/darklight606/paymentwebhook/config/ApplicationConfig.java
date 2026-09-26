package io.github.darklight606.paymentwebhook.config;

import io.github.darklight606.paymentwebhook.adapter.in.rest.WebhookBodySizeFilter;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AcmePayWebhookProperties.class)
public class ApplicationConfig {

    private static final String WEBHOOK_PATH = "/webhooks/acmepay";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public FilterRegistrationBean<WebhookBodySizeFilter> webhookBodySizeFilter(AcmePayWebhookProperties properties) {
        var registration = new FilterRegistrationBean<>(
                new WebhookBodySizeFilter(properties.maxBodyBytes().toBytes()));
        registration.addUrlPatterns(WEBHOOK_PATH);
        return registration;
    }
}
