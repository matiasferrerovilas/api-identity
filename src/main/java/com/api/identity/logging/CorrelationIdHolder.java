package com.api.identity.logging;

import org.slf4j.MDC;

/**
 * Reads the correlation id {@link CorrelationIdFilter} put in MDC for the current request, so event
 * publishers can stamp RabbitMQ payloads with it without needing a request-scoped bean injected
 * through every service in the call chain.
 */
public final class CorrelationIdHolder {

    private CorrelationIdHolder() {
    }

    public static String current() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
