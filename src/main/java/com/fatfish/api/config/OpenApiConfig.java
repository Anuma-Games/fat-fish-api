package com.fatfish.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI fatFishOpenApi() {
        return new OpenAPI().info(new Info()
                        .title("Fat Fish API")
                        .version("v1")
                        .description("API del videojuego Fat Fish (IDGS101N, UTCH): registro de jugadores y partidas "
                                + "desde el juego, y consultas e inserciones de la PWA."))
                .components(new Components()
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key"))
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }

    /**
     * Marca con bearerAuth las operaciones que reciben el token del jugador en Authorization, así Swagger
     * manda el token desde el botón Authorize solo donde hace falta.
     */
    @Bean
    OperationCustomizer bearerAuthOnTokenOperations() {
        return (operation, handlerMethod) -> {
            boolean usesToken = Arrays.stream(handlerMethod.getMethodParameters())
                    .map(parameter -> parameter.getParameterAnnotation(RequestHeader.class))
                    .anyMatch(header -> header != null && HttpHeaders.AUTHORIZATION.equals(header.name()));
            if (usesToken) {
                operation.addSecurityItem(new SecurityRequirement().addList("apiKey").addList("bearerAuth"));
            }
            return operation;
        };
    }
}
