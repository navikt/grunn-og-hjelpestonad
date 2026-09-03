package no.nav.gjenlevende.bs.sak.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local-mock")
open class MockSwaggerConfig {
    @Bean
    @Primary
    open fun mockSwaggerApiConfig(): OpenAPI =
        OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes("bearerAuth", bearerSecurityScheme()),
            ).addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .info(
                Info()
                    .title("Gjenlevende BS Sak (Mock)")
                    .description(
                        """
                        Swagger for Gjenlevende-BS-Sak - Mock profil

                        Hent token med:
                        ```
                        curl -s -X POST http://localhost:8089/default/token \
                          -d 'grant_type=client_credentials&client_id=test&client_secret=test' \
                          | jq -r '.access_token'
                        ```

                        Klikk "Authorize" og lim inn token.
                        """.trimIndent(),
                    ).version("1.0.0-mock")
                    .contact(
                        Contact()
                            .name("Team Etterlatte")
                            .url("https://github.com/navikt/gjenlevende-bs-sak"),
                    ),
            )

    private fun bearerSecurityScheme(): SecurityScheme =
        SecurityScheme()
            .name("bearerAuth")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Lim inn JWT token fra mock-oauth2-server")
}
