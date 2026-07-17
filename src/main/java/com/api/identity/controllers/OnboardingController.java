package com.api.identity.controllers;

import com.api.identity.services.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/onboarding")
@Tag(name = "Onboarding", description = "API de onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @Operation(
            summary = "Marcar tour como visto",
            description = "Marca que el usuario autenticado ya vio el tour de la aplicación. "
                    + "Esto evita que se muestre el tour en futuros ingresos.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Tour marcado como visto"
                    )
            }
    )
    @PutMapping("/tour")
    public ResponseEntity<Void> markTourAsSeen(@RequestHeader("X-Source-Service") String sourceService) {
        onboardingService.markTourAsSeen(sourceService);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Marcar first-login como completado",
            description = "Marca que el usuario indicado ya completó su primer ingreso.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "First-login marcado como completado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario inexistente"
                    )
            }
    )
    @PatchMapping("/{userId}/first-login")
    public ResponseEntity<Void> changeUserFirstLoginStatus(@PathVariable Long userId) {
        onboardingService.changeUserFirstLoginStatus(userId);
        return ResponseEntity.noContent().build();
    }
}
