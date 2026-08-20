package com.api.identity.events;

import java.time.LocalDateTime;

/**
 * Published to RabbitMQ (see {@link com.api.identity.configuration.RabbitConfig}) when a member is
 * removed from a workspace (kicked, not a voluntary leave), so sibling services (api-movements,
 * api-keep) know someone just lost access to a shared workspace.
 */
public record MemberRemovedEvent(
        Long workspaceId,
        String workspaceName,
        String removedByEmail,
        String removedUserEmail,
        LocalDateTime removedAt) {
}
