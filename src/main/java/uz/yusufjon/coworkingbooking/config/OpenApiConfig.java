package uz.yusufjon.coworkingbooking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Coworking Booking API",
                version = "v1",
                description = "REST API for authentication, room management, user administration, and coworking booking operations."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Provide a valid JWT access token. Swagger UI will send it as an Authorization Bearer token."
)
public class OpenApiConfig {
}
