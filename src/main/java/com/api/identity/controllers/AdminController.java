package com.api.identity.controllers;

import com.api.identity.records.admin.AdminUserSummaryDTO;
import com.api.identity.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints administrativos — todo bajo /v1/admin requiere ROLE_ADMIN, forzado a nivel de
 * SecurityConfiguration (no solo acá), así que un nuevo método en este controller queda
 * protegido aunque alguien se olvide de anotarlo. Mismo patrón que api-movements.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin")
@Tag(name = "Admin", description = "Endpoints administrativos — requieren ROLE_ADMIN")
public class AdminController {

    private final UserService userService;

    @Operation(
            summary = "Listar todos los usuarios y sus workspaces",
            description = "Devuelve todos los usuarios de la instancia, cada uno con los workspaces "
                    + "activos a los que pertenece y su rol en cada uno. No existe otra forma de ver esto "
                    + "hoy: GET /v1/users necesita ids de antemano y GET /v1/workspaces/members solo "
                    + "devuelve los workspaces del usuario autenticado.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Usuarios obtenidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdminUserSummaryDTO.class)
                    )
            )
    )
    @GetMapping("/users")
    public List<AdminUserSummaryDTO> listUsers() {
        return userService.listAllUsersWithWorkspaces();
    }
}
