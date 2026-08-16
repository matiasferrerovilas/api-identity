package com.api.identity.unit.services.api

import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.services.api.SourceServiceResolver
import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

import java.time.Instant

class SourceServiceResolverTest extends Specification {

    SourceServiceResolver resolver = new SourceServiceResolver()

    def "resolve - returns the app claim from the token"() {
        given:
        def jwt = jwtWithApp("api-keep")

        expect:
        resolver.resolve(jwt) == "api-keep"
    }

    def "resolve - throws PermissionDeniedException when the app claim is missing"() {
        given:
        def jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("azp", "some-client-without-the-mapper-configured")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build()

        when:
        resolver.resolve(jwt)

        then:
        thrown(PermissionDeniedException)
    }

    private static Jwt jwtWithApp(String app) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("app", app)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build()
    }
}
