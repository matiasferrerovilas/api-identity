package com.api.identity.events;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (see {@link com.api.identity.configuration.RabbitConfig}) when a
 * workspace invitation is accepted, so sibling services (api-movements, api-keep) know someone
 * just gained access to a shared workspace instead of finding out on their next unrelated request.
 */
public record InvitationAcceptedEvent(
        Long invitationId,
        Long workspaceId,
        String workspaceName,
        String acceptedByEmail,
        LocalDateTime acceptedAt) {
}
