package br.com.abba.soft.mymoney.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "My Money API",
                version = "v1",
                description = "APIs para gerenciar despesas pessoais, incluindo cadastro de usuários e operações de despesas.",
                contact = @Contact(name = "Equipe My Money", email = "support@example.com"),
                license = @License(name = "MIT")
        )
)
public class OpenApiConfig {
}
