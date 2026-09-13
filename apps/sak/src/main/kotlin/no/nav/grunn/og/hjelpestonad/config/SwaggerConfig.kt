package no.nav.grunn.og.hjelpestonad.config

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
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Server-listen settes uavhengig av profil slik at /v3/api-docs blir likt
 * lokalt og i deployede miljøer. Uten dette får OpenAPI-dokumentet en
 * auto-utledet server-URL lokalt, og genererte frontend-typer blir
 * avhengige av hvilken backend de ble generert fra.
 */
@Configuration
open class OpenApiServerConfig {
    @Bean
    open fun relativeServerCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.servers(listOf(Server().url("/").description("Samme opphav som API-et")))
        }
}

@Configuration
@Profile("!local-mock")
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
            .addSecurityItem(SecurityRequirement().addList("oauth2", listOf("read", "write")))
            .info(
                Info()
                    .title("Grunn- og hjelpestønad Sak")
                    .description("Swagger for Grunn- og hjelpestønad")
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
