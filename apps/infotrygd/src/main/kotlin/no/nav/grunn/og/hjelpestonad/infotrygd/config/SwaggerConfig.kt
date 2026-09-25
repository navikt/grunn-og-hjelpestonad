package no.nav.grunn.og.hjelpestonad.infotrygd.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class SwaggerConfig(
    @Value("\${azure.authorization-url}")
    val authorizationUrl: String,
    @Value("\${azure.token-endpoint-url}")
    val tokenUrl: String,
    @Value("\${azure.api-scope}")
    val apiScope: String,
) {
    @Bean
    open fun swaggerApiConfig(): OpenAPI =
        OpenAPI()
            .components(Components().addSecuritySchemes("oauth2", securitySchemes()))
            .addSecurityItem(SecurityRequirement().addList("oauth2", listOf(apiScope)))
            .servers(listOf(Server().url("/").description("Samme opphav som API-et")))
            .info(
                Info()
                    .title("Grunn- og hjelpestønad Infotrygd")
                    .description("Swagger for Grunn- og hjelpestønad Infotrygd")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Team grunn- og hjelpestønad")
                            .url("https://github.com/navikt/grunn-og-hjelpestonad"),
                    ),
            )

    private fun securitySchemes(): SecurityScheme =
        SecurityScheme()
            .name("oauth2")
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("oauth2")
            .`in`(SecurityScheme.In.HEADER)
            .flows(
                OAuthFlows()
                    .authorizationCode(
                        OAuthFlow()
                            .authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(Scopes().addString(apiScope, "read,write")),
                    ),
            )
}
