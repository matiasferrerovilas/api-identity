package com.api.identity.controllers;

import com.api.identity.records.workspaces.AddWorkspaceRecord;
import com.api.identity.records.workspaces.WorkspaceAdded;
import com.api.identity.records.workspaces.WorkspaceDTO;
import com.api.identity.services.WorkspaceAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/workspaces")
@Tag(name = "Workspaces", description = "API de workspaces")
@RequiredArgsConstructor
@Validated
public class WorkspaceController {

    private final WorkspaceAddService workspaceAddService;

    @Operation(
            summary = "Crear workspaces para un usuario",
            description = "Crea en bloque los workspaces indicados para el usuario, registrándolo como OWNER de cada uno. "
                    + "Usado por otros servicios durante el onboarding.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Workspaces creados",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceAdded.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Lista vacía, nombre en blanco o nombre repetido"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario inexistente"
                    )
            }
    )
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public List<WorkspaceAdded> createWorkspaces(@Valid AddWorkspaceRecord workspaces) {
        return workspaceAddService.createWorkspaces(workspaces);
    }

    @Operation(
            summary = "Crear workspaces para un usuario",
            description = "Crea en bloque los workspaces indicados para el usuario, registrándolo como OWNER de cada uno. "
                    + "Usado por otros servicios durante el onboarding.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Workspaces creados",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceAdded.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Lista vacía, nombre en blanco o nombre repetido"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario inexistente"
                    )
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceDTO> getWorkspaces(Long userId) {
        return workspaceAddService.getWorkspaces(userId);
    }
}
