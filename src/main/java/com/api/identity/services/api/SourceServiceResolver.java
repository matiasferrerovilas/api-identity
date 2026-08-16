package com.api.identity.services.api;

import com.api.identity.exceptions.PermissionDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves which app is calling from the JWT's {@code app} claim instead of trusting a
 * client-supplied header. This claim is set per-client in Keycloak via a hardcoded-claim protocol
 * mapper (not derived here), so it's covered by the token signature and can't be misrepresented
 * by whichever backend is forwarding the user's token — unlike the old X-Source-Service header,
 * which any caller could set to any value.
 */
@Component
public class SourceServiceResolver {

    private static final String APP_CLAIM = "app";

    public String resolve(Jwt jwt) {
        String app = jwt.getClaimAsString(APP_CLAIM);

        if (app == null) {
            throw new PermissionDeniedException("Falta el claim '%s' en el token".formatted(APP_CLAIM));
        }

        return app;
    }
}
