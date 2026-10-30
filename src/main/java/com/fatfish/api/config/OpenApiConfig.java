package com.fatfish.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI fatFishOpenApi() {
        return new OpenAPI().info(new Info()
                        .title("Fat Fish API")
                        .version("v1")
                        .description("API del videojuego Fat Fish (IDGS101N, UTCH): registro de jugadores y partidas "
                                + "desde el juego, y consultas e inserciones de la PWA."))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Api-Key")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
