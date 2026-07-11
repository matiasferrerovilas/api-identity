package com.api.identity.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<@NonNull Jwt, @NonNull AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.principle-attribute}")
    private String principleAttribute;

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {

        Collection<GrantedAuthority> authorities = Stream
                .concat(jwtGrantedAuthoritiesConverter.convert(jwt).stream(), extractResourceRoles(jwt).stream())
                .collect(Collectors.toSet());

        String principalName = getPrincipleClaimName(jwt);
        if (principalName == null) {
            log.warn("El claim principal ('{}') no se encontró en el token JWT. Intentando usar 'sub' como fallback.", principleAttribute);
            principalName = jwt.getClaimAsString(JwtClaimNames.SUB);
            if (principalName == null) {
                log.error("""
                        Ni el claim principal configurado ('{}') ni 'sub' se encontraron en el token JWT.
                        Esto puede causar problemas de autenticación.
                        Usando el ID del JWT como último recurso.""", principleAttribute);
                principalName = jwt.getId();
            }
        }

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                principalName
        );
    }


    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> realmAccess = Optional.ofNullable(jwt.getClaimAsMap(REALM_ACCESS_CLAIM))
                .orElseGet(() -> {
                    log.debug("El claim '{}' no está presente o es nulo en el JWT. No se extraerán roles de reino.", REALM_ACCESS_CLAIM);
                    return Collections.emptyMap();
                });

        @SuppressWarnings("unchecked")
        Collection<String> realmRoles = Optional.ofNullable((Collection<String>) realmAccess.get(ROLES_CLAIM))
                .orElseGet(() -> {
                    log.debug("El claim '{}' para el reino no está presente o es nulo. No se extraerán roles de reino.", ROLES_CLAIM);
                    return Collections.emptyList();
                });

        if (realmRoles.isEmpty()) {
            log.info("No se encontraron roles de reino en el token JWT.");
        }

        return realmRoles.stream()
                .filter(role -> role.startsWith(ROLE_PREFIX))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }


    private String getPrincipleClaimName(Jwt jwt) {
        return Optional.ofNullable(principleAttribute)
                .map(jwt::getClaimAsString)
                .orElseGet(() -> {
                    log.debug("El principle-attribute configurado '{}' no está presente o es nulo. Usando 'sub' como fallback.", principleAttribute);
                    return jwt.getClaimAsString(JwtClaimNames.SUB);
                });
    }
}