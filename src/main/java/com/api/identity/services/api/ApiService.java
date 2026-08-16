package com.api.identity.services.api;

import com.api.identity.entities.Api;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.repositories.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiService {

    // The X-Source-Service header used to be trusted verbatim from any authenticated caller,
    // letting anyone spam fake "apis" rows (and the OnboardingDone rows tied to them) just by
    // sending an arbitrary header value. api-movements is the only real caller today (see
    // SOURCE_SERVICE_NAME in that repo's IdentityClientConfig) — reject anything else instead of
    // auto-creating a row for it.
    private static final Set<String> KNOWN_SOURCE_SERVICES = Set.of("api-movements");

    private final ApiRepository apiRepository;

    @Transactional
    public Api getOrCreate(String name) {
        if (!KNOWN_SOURCE_SERVICES.contains(name)) {
            throw new PermissionDeniedException("Servicio de origen desconocido: '%s'".formatted(name));
        }

        return apiRepository.findByName(name)
                .orElseGet(() -> apiRepository.save(Api.builder().name(name).build()));
    }
}
