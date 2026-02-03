package com.trackspace.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI Configuration
 * 
 * Configures API documentation with Swagger UI
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI trackSpaceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackSpace API")
                        .description("API documentation for TrackSpace - Project Management Support Tool")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TrackSpace Team")
                                .email("support@trackspace.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
