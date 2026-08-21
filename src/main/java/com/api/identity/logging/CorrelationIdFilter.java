package com.api.identity.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stamps every request with a correlation id — reused from an incoming {@value #HEADER_NAME}
 * header when the caller (a gateway app) already has one, generated fresh otherwise. Put in MDC so
 * every log line for the request carries it (see {@code logging.pattern.level} in
 * application.yaml), echoed back as a response header, and read via {@link CorrelationIdHolder} by
 * the RabbitMQ event publishers so payloads carry it too — closes the "no way to trace a request
 * across services" gap.
 *
 * <p>An incoming value is only trusted if it looks like an id (bounded length, safe charset) —
 * otherwise a fresh one is generated instead. This isn't primarily about api-identity itself (not
 * internet-exposed today), but about not blindly stamping an attacker-controlled string into every
 * log line and MDC value if that ever changes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";

    private static final int MAX_LENGTH = 100;
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._-]+$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (!isValid(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean isValid(String value) {
        return value != null && value.length() <= MAX_LENGTH && VALID_ID.matcher(value).matches();
    }
}
