package com.api.identity.controllers;

import com.api.identity.records.UserMe;
import com.api.identity.records.UserTypeUpdateRequest;
import com.api.identity.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "API de usuarios")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Obtener datos del usuario autenticado",
            description = "Retorna el ID interno, email, estado de onboarding y tipo de usuario del usuario autenticado. "
                    + "Si el usuario no existe aún en la base de datos, retorna isFirstLogin=true con los demás campos en null.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Datos del usuario autenticado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserMe.class)
                            )
                    )
            }
    )
    @GetMapping("/me")
    public UserMe getMe() {
        return userService.getMe();
    }

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
    public ResponseEntity<Void> markTourAsSeen() {
        userService.markTourAsSeen();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cambiar tipo de usuario (solo ADMIN)",
            description = "Permite a un administrador cambiar su propio tipo de usuario entre PERSONAL y ENTERPRISE. "
                    + "Si el tipo de usuario ya es el solicitado, no realiza ningún cambio.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Tipo de usuario actualizado exitosamente o sin cambios necesarios"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Tipo de usuario inválido o no proporcionado"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuario no tiene rol ADMIN"
                    )
            }
    )
    @PatchMapping("/me/type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserType(@Valid @RequestBody UserTypeUpdateRequest request) {
        userService.changeUserType(request.userType());
        return ResponseEntity.noContent().build();
    }

}
