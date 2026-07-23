package com.api.identity.controllers;

import com.api.identity.records.user.UserMe;
import com.api.identity.records.user.UserToAdd;
import com.api.identity.records.user.UserTypeUpdateRequest;
import com.api.identity.services.user.UserAddService;
import com.api.identity.services.user.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "API de usuarios")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserAddService userAddService;

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
    public UserMe getMe(@RequestHeader("X-Source-Service") String sourceService) {
        return userService.getMe(sourceService);
    }

    @Operation(
            summary = "Obtener usuarios por ID",
            description = "Retorna los datos básicos de los usuarios cuyos IDs fueron indicados. "
                    + "Usado por otros servicios para resolver referencias a usuarios.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuarios encontrados",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserMe.class)
                            )
                    )
            }
    )
    @GetMapping
    public List<UserMe> getUsersByIds(@RequestParam List<Long> ids) {
        return userService.getUsersByIds(ids);
    }

    @Operation(
            summary = "Crear usuario",
            description = "Crea al usuario con los datos recibidos si no existe (por email) y le asegura el registro "
                    + "de onboarding para la API que llama. Si el usuario ya existe (por haber hecho onboarding en "
                    + "otra API) solo se crea el onboarding para la API actual, reutilizando el usuario existente. "
                    + "Si ya existe un onboarding para ese usuario y esa API, falla con conflicto.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Datos del usuario creado o reutilizado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserMe.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Ya existe un onboarding para ese usuario en esta API"
                    )
            }
    )
    @PostMapping
    public UserMe createLogInUser(@RequestBody UserToAdd request, @RequestHeader("X-Source-Service") String sourceService) {
        return userAddService.createLogInUser(request, sourceService);
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
