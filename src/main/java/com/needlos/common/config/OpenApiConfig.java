package com.needlos.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI needlosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NeedlOS API")
                        .version("0.1.0")
                        .description("ERP multi-tenant para sastrerias. "
                                + "Obten un token en POST /api/auth/login y pulsa 'Authorize'."))
                .components(new Components().addSecuritySchemes("Bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
