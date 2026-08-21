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

    def "generates a new id when the header exceeds the max length"() {
        given:
        def request = new MockHttpServletRequest()
        def tooLong = "a" * 101
        request.addHeader(HEADER, tooLong)
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) != tooLong
        response.getHeader(HEADER).length() < tooLong.length()
    }

    def "generates a new id when the header contains a CRLF log-injection attempt"() {
        given:
        def request = new MockHttpServletRequest()
        def malicious = "trace-1\r\nfake-log-line: injected"
        // MockHttpServletRequest stores whatever we give it; a real servlet container would reject
        // a raw CRLF in a header value before this filter ever ran, but validating defensively here
        // means we don't rely on that.
        request.addHeader(HEADER, malicious)
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) != malicious
        !response.getHeader(HEADER).contains("\r")
        !response.getHeader(HEADER).contains("\n")
    }

    def "generates a new id when the header contains characters outside the safe charset"() {
        given:
        def request = new MockHttpServletRequest()
        request.addHeader(HEADER, "<script>alert(1)</script>")
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) != "<script>alert(1)</script>"
    }

    def "accepts a UUID-shaped header"() {
        given:
        def request = new MockHttpServletRequest()
        def uuid = "b3d1c2a4-5e6f-4a1b-9c8d-1234567890ab"
        request.addHeader(HEADER, uuid)
        def response = new MockHttpServletResponse()

        when:
        filter.doFilter(request, response, new MockFilterChain())

        then:
        response.getHeader(HEADER) == uuid
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
