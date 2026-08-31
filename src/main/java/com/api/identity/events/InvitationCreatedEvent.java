package com.api.identity.events;

import com.api.identity.enums.WorkspaceRole;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (see {@link com.api.identity.configuration.RabbitConfig}) when a
 * workspace invitation is sent, so the invited user's own app (resolved from their email, not
 * this event) can push them a live notification instead of requiring a poll/refresh.
 */
public record InvitationCreatedEvent(
        Long invitationId,
        Long workspaceId,
        String workspaceName,
        String invitedByEmail,
        String invitedUserEmail,
        WorkspaceRole role,
        LocalDateTime createdAt,
        String correlationId) {
}
