package com.api.identity.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("api-identity API")
                        .description("""
                                Servicio de identidad compartido por toda la suite M2 (movements, keep).

                                **Funcionalidades:**
                                • Auto-provisión de usuarios en el primer login (por app)
                                • Workspaces y membresías (OWNER / COLLABORATOR / READ_ONLY)
                                • Invitaciones a workspaces, con notificación por RabbitMQ
                                • Resolución de la app llamante a partir del claim `app` del JWT

                                **Autenticación:** JWT Bearer Token (OAuth2, Keycloak realm `m2`)

                                No está expuesto a internet — solo lo consumen api-movements y api-keep.
                                """)
                        .version("1.2.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("api-support@movement.eva-core.com")));
    }
}
