package com.api.identity.configuration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * api-identity only publishes onto this exchange (workspace/invitation events) — it has no
 * listeners of its own. Consumers (api-movements, api-keep) declare their own durable queue
 * bound to {@link #IDENTITY_TOPIC_EXCHANGE} with the routing key they care about; they also
 * declare this same exchange (same name/type) so binding works regardless of which service
 * starts first.
 */
@Configuration
public class RabbitConfig {

    public static final String IDENTITY_TOPIC_EXCHANGE = "identity.topic";
    public static final String ROUTING_KEY_INVITATION_SENT = "identity.invitation.sent";
    public static final String ROUTING_KEY_INVITATION_ACCEPTED = "identity.invitation.accepted";
    public static final String ROUTING_KEY_MEMBER_REMOVED = "identity.member.removed";

    @Bean
    public JacksonJsonMessageConverter jackson2JsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    TopicExchange identityTopicExchange() {
        return new TopicExchange(IDENTITY_TOPIC_EXCHANGE);
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
