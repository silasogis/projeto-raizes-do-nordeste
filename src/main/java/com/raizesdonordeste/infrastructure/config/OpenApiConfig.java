package com.raizesdonordeste.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Raízes do Nordeste — API Back-End")
                        .description("API RESTful para gestão de pedidos, pagamentos mock, cardápio por unidade, controle de estoque e auditoria da rede Raízes do Nordeste.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Raízes do Nordeste")
                                .email("suporte@raizesdonordeste.com.br"))
                        .license(new License()
                                .name("Uso Acadêmico - Projeto Multidisciplinar")
                                .url("https://github.com/silasogeo/projeto_backend_raizes_nordeste")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT gerado no endpoint /auth/login para autenticar as requisições protegidas.")));
    }
}
