package com.api.identity.services.api;

import com.api.identity.entities.Api;
import com.api.identity.repositories.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiService {

    // "name" is expected to already come from SourceServiceResolver (derived from the JWT's
    // signed app claim), not from a client-supplied header — this is no longer the place that
    // decides which service names are legitimate.
    private final ApiRepository apiRepository;

    @Transactional
    public Api getOrCreate(String name) {
        return apiRepository.findByName(name)
                .orElseGet(() -> apiRepository.save(Api.builder().name(name).build()));
    }
}
