package com.api.identity.services.workspace;

import com.api.identity.configuration.RabbitConfig;
import com.api.identity.events.MemberRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceMembershipEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishMemberRemoved(MemberRemovedEvent event) {
        log.debug("Publicando remoción de {} del workspace {} (por {})",
                event.removedUserEmail(), event.workspaceId(), event.removedByEmail());
        rabbitTemplate.convertAndSend(
                RabbitConfig.IDENTITY_TOPIC_EXCHANGE,
                RabbitConfig.ROUTING_KEY_MEMBER_REMOVED,
                event);
    }
}
