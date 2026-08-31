package com.api.identity.controllers;

import com.api.identity.records.invitations.AcceptRejectInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceSendInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceSentInvitationDTO;
import com.api.identity.services.invitations.WorkspaceInvitationService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/invitations")
@Tag(name = "Workspace Invitations", description = "API de invitaciones a workspaces")
@RequiredArgsConstructor
@Validated
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService workspaceInvitationService;

    @Operation(
            summary = "Obtener invitaciones pendientes",
            description = "Devuelve las invitaciones a workspaces pendientes de aceptación para el usuario autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Invitaciones obtenidas",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceInvitationDTO.class)
                            )
                    )
            }
    )
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceInvitationDTO> getPendingInvitations() {
        return workspaceInvitationService.getPendingInvitations();
    }

    @Operation(
            summary = "Enviar invitación a un workspace",
            description = "Envía una invitación al workspace indicado a cada uno de los emails informados. "
                    + "El usuario autenticado debe pertenecer al workspace. Los emails con una invitación "
                    + "pendiente ya existente para ese workspace son ignorados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones enviadas correctamente"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "El usuario autenticado no pertenece al workspace indicado"
                    )
            }
    )
    @PostMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.OK)
    public void sendInvitation(@PathVariable Long workspaceId, @Valid @RequestBody WorkspaceSendInvitationDTO body) {
        workspaceInvitationService.sendInvitation(workspaceId, body);
    }

    @Operation(
            summary = "Obtener invitaciones enviadas",
            description = "Devuelve todas las invitaciones a workspaces enviadas por el usuario autenticado, "
                    + "más reciente primero, en cualquier estado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Invitaciones obtenidas",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = WorkspaceSentInvitationDTO.class)
                            )
                    )
            }
    )
    @GetMapping("/sent")
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        return workspaceInvitationService.getSentInvitations();
    }

    @Operation(
            summary = "Cancelar una invitación enviada",
            description = "Cancela una invitación pendiente enviada por el usuario autenticado, antes de que el "
                    + "destinatario la acepte o rechace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitación cancelada"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Invitación inexistente o no enviada por el usuario autenticado"
                    )
            }
    )
    @DeleteMapping("/{invitationId}")
    @ResponseStatus(HttpStatus.OK)
    public void cancelInvitation(@PathVariable Long invitationId) {
        workspaceInvitationService.cancelInvitation(invitationId);
    }

    @Operation(
            summary = "Aceptar o rechazar invitación",
            description = "Acepta o rechaza, según el campo status, la invitación indicada por id, "
                    + "recibida por el usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitación actualizada correctamente")
            }
    )
    @PatchMapping()
    @ResponseStatus(HttpStatus.OK)
    public void acceptRejectInvitation(@Valid @RequestBody AcceptRejectInvitationDTO invitationDTO) {
        workspaceInvitationService.acceptRejectInvitation(invitationDTO);
    }
}
