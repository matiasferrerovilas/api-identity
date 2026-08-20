package com.api.identity.services.invitations;

import com.api.identity.configuration.RabbitConfig;
import com.api.identity.events.InvitationAcceptedEvent;
import com.api.identity.events.InvitationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInvitationCreated(InvitationCreatedEvent event) {
        log.debug("Publicando invitación {} a workspace {} para {}", event.invitationId(), event.workspaceId(), event.invitedUserEmail());
        rabbitTemplate.convertAndSend(
                RabbitConfig.IDENTITY_TOPIC_EXCHANGE,
                RabbitConfig.ROUTING_KEY_INVITATION_SENT,
                event);
    }

    public void publishInvitationAccepted(InvitationAcceptedEvent event) {
        log.debug("Publicando invitación {} aceptada por {} en workspace {}",
                event.invitationId(), event.acceptedByEmail(), event.workspaceId());
        rabbitTemplate.convertAndSend(
                RabbitConfig.IDENTITY_TOPIC_EXCHANGE,
                RabbitConfig.ROUTING_KEY_INVITATION_ACCEPTED,
                event);
    }
}
