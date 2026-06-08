package com.banco.bancoapi.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bancoApiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Banco API")
                .description("API REST de banco digital: transferencias atomicas e seguras sob "
                        + "concorrencia (lock pessimista + ordenacao de locks), idempotencia e "
                        + "notificacao assincrona pos-commit.")
                .version("1.0.0")
                .license(new License().name("MIT")));
    }
}
