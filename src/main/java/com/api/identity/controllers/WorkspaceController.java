package com.api.identity.controllers;

import com.api.identity.records.workspaces.AddWorkspaceRecord;
import com.api.identity.records.workspaces.WorkspaceAdded;
import com.api.identity.records.workspaces.WorkspaceDTO;
import com.api.identity.records.workspaces.WorkspaceMemberDTO;
import com.api.identity.services.workspace.WorkspaceAddService;
import com.api.identity.services.workspace.WorkspaceMembershipService;
import com.api.identity.services.workspace.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final WorkspaceService workspaceService;
    private final WorkspaceMembershipService workspaceMembershipService;

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
    public List<WorkspaceAdded> createWorkspaces(@Valid @RequestBody List<AddWorkspaceRecord> workspacesToAdd) {
        return workspaceAddService.createWorkspaces(workspacesToAdd);
    }

    @Operation(
            summary = "Obtener miembros de mis workspaces",
            description = "Devuelve todos los WorkspaceMember de los workspaces donde el usuario autenticado "
                    + "es owner o miembro.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Miembros obtenidos",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceMemberDTO.class)
                            )
                    )
            }
    )
    @GetMapping("/members")
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceMemberDTO> getWorkspaceMembers() {
        return workspaceService.getWorkspaceMembers();
    }

    @Operation(
            summary = "Verificar pertenencia a un workspace",
            description = "Verifica que el usuario indicado pertenezca al workspace indicado. "
                    + "Usado por otros servicios para validar autorización.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "El usuario pertenece al workspace"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "El usuario no pertenece al workspace indicado"
                    )
            }
    )
    @GetMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void verifyMembership(@PathVariable Long workspaceId, @PathVariable Long userId) {
        workspaceMembershipService.verifyMembership(workspaceId, userId);
    }

    @Operation(
            summary = "Obtener workspace por id",
            description = "Devuelve los datos del workspace indicado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Workspace obtenido",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Workspace inexistente"
                    )
            }
    )
    @GetMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.OK)
    public WorkspaceDTO getWorkspaceById(@PathVariable Long workspaceId) {
        return workspaceService.getWorkspaceDTOById(workspaceId);
    }

    @Operation(
            summary = "Eliminar un workspace",
            description = "Elimina el workspace indicado. Requiere que el usuario autenticado pertenezca a dicho workspace.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Workspace eliminado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "El usuario no pertenece al workspace indicado"
                    )
            }
    )
    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteWorkspace(@PathVariable Long workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
    }

}
