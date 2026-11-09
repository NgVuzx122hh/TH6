package vn.th6.appointment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI appointmentOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Appointment and Examination Service API").version("v1"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
