package com.api.identity.configuration;

import com.api.identity.events.InvitationAcceptedEvent;
import com.api.identity.events.InvitationCreatedEvent;
import com.api.identity.events.MemberRemovedEvent;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Bajo native-image hace falta reflection registrada explícitamente para los records publicados
 * solo vía RabbitMQ ({@code RabbitTemplate.convertAndSend} con {@code Object} genérico, ver
 * {@code InvitationEventPublisher} y {@code WorkspaceMembershipEventPublisher}) — ese camino no
 * pasa por el escaneo AOT de Spring MVC (que solo descubre tipos alcanzables desde el return de
 * un método de {@code @RestController}), así que quedan sin registrar hasta que efectivamente se
 * disparan en runtime: un tipo así tira {@code UnsupportedFeatureError: Record components not
 * available} en el native image de producción, nunca en tests porque ahí no corre como native
 * image. Mismo bug encontrado y corregido primero en api-movements ({@code NotificationRecord}) y
 * luego proactivamente en api-keep.
 */
public class WebBindingRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] RECORD_TYPES = {
            InvitationCreatedEvent.class,
            InvitationAcceptedEvent.class,
            MemberRemovedEvent.class,
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> type : RECORD_TYPES) {
            hints.reflection().registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
