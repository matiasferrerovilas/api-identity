package com.api.identity.unit.logging

import com.api.identity.logging.CorrelationIdFilter
import com.api.identity.logging.CorrelationIdHolder
import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification

class CorrelationIdFilterTest extends Specification {

    private static final String HEADER = CorrelationIdFilter.HEADER_NAME

    CorrelationIdFilter filter = new CorrelationIdFilter()

    def cleanup() {
        MDC.clear()
    }

    def "generates a new correlation id and echoes it back when the caller sends none"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) != null
        !response.getHeader(HEADER).isBlank()
    }

    def "reuses the caller's correlation id instead of generating a new one"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader(HEADER, "caller-supplied-id")
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) == "caller-supplied-id"
    }

    def "generates a new id when the header is present but blank"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader(HEADER, "   ")
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) != "   "
        !response.getHeader(HEADER).isBlank()
    }

    def "exposes the correlation id via CorrelationIdHolder for the duration of the request, then clears it"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader(HEADER, "trace-123")
        def response = new MockHttpServletResponse()
        String seenDuringRequest = null
        FilterChain chain = { req, res -> seenDuringRequest = CorrelationIdHolder.current() } as FilterChain

        when:
        filter.doFilter(request, response, chain)

        then:
        seenDuringRequest == "trace-123"
        CorrelationIdHolder.current() == null
    }
}
