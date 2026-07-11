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
                        .title("Movement API")
                        .description("""
                                API para la gestión de finanzas personales.

                                **Funcionalidades:**
                                • Movimientos (ingresos/gastos) con importación desde PDF bancario
                                • Control de suscripciones y servicios recurrentes
                                • Workspaces compartidos con invitaciones
                                • Balance por período, categoría y cuenta
                                • Tasas de cambio automáticas

                                **Autenticación:** JWT Bearer Token (OAuth2)
                                """)
                        .version("2.0.1")
                        .contact(new Contact()
                                .name("API Support")
                                .email("api-support@movement.eva-core.com")));
    }
}
